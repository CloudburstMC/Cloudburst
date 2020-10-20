package org.cloudburstmc.server.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.nukkitx.protocol.bedrock.packet.CreativeContentPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.item.*;
import org.cloudburstmc.server.item.behavior.*;
import org.cloudburstmc.server.item.data.*;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.DefaultItemSerializer;
import org.cloudburstmc.server.item.serializer.ItemSerializer;
import org.cloudburstmc.server.item.serializer.RecordSerializer;
import org.cloudburstmc.server.item.serializer.TreeSpeciesSerializer;
import org.cloudburstmc.server.potion.Potion;
import org.cloudburstmc.server.utils.Config;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
public class CloudItemRegistry implements ItemRegistry {
    private static final CloudItemRegistry INSTANCE;
    private static final List<ItemData> VANILLA_ITEMS;

    static {
        InputStream stream = RegistryUtils.getOrAssertResource("data/runtime_item_states.json"); //TODO: use legacy_item_ids.json instead

        try {
            VANILLA_ITEMS = Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<List<ItemData>>() {
            });
        } catch (IOException e) {
            throw new AssertionError("Unable to load vanilla items", e);
        }

        INSTANCE = new CloudItemRegistry(BlockRegistry.get()); // Needs to be initialized afterwards
    }

    private final Reference2ReferenceMap<Identifier, ItemType> typeMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<Class<?>, ItemDataSerializer<?>> dataSerializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemBehavior> behaviorMap = new Reference2ObjectOpenHashMap<>();
    private final List<ItemStack> creativeItems = new ArrayList<>();
    private final BiMap<Integer, Identifier> runtimeIdMap = HashBiMap.create();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger();
    private int lastLegacyId;
    private final BlockRegistry blockRegistry;
    private List<StartGamePacket.ItemEntry> itemEntries;
    private volatile CreativeContentPacket creativeContent;

    private volatile boolean closed;

    private CloudItemRegistry(BlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
        try {
            this.registerVanillaItems();
            this.registerVanillaIdentifiers();
            this.registerVanillaDataSerializers();

            this.registerVanillaCreativeItems();
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }

        // register missing vanilla items.
//        for (ItemData item : VANILLA_ITEMS) {
//            if (item.id < 256) {
//                continue;
//            }
//            Identifier identifier = Identifier.fromString(item.name);
//            if (!this.typeMap.containsKey(identifier)) {
//                log.debug("Non-implemented item found {}", identifier);
//                registerVanilla(identifier, SimpleItem::new, item.id);
//            }
//        }
    }

    public static CloudItemRegistry get() {
        return INSTANCE;
    }

    public synchronized <T> void registerDataSerializer(Class<T> metadataClass, ItemDataSerializer<T> serializer) {
        Preconditions.checkNotNull(metadataClass, "metadataClass");
        Preconditions.checkNotNull(serializer, "serializer");
        this.dataSerializers.put(metadataClass, serializer);
    }

    @Override
    public synchronized void register(ItemType type, ItemSerializer serializer, ItemBehavior behavior, Identifier... identifiers) throws RegistryException {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(behavior, "behavior");
        checkClosed();

        if (identifiers == null || identifiers.length <= 0) {
            identifiers = new Identifier[]{type.getId()};
        }

        if (this.typeMap.containsValue(type)) {
            throw new RegistryException(type + " has already been registered");
        }

        for (Identifier identifier : identifiers) {
            if (this.typeMap.containsKey(identifier)) {
                throw new RegistryException(identifier + " has already been registered");
            }
        }

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        for (Identifier identifier : identifiers) {
            this.typeMap.put(identifier, type);
            this.runtimeIdMap.put(this.runtimeIdAllocator.getAndIncrement(), identifier);
        }

        this.behaviorMap.put(type, behavior);
    }

    private synchronized void registerVanilla(ItemType type, int legacyId) throws RegistryException {
        registerVanilla(type, null, NoopItemBehavior.INSTANCE, legacyId);
    }

    private synchronized void registerVanilla(ItemType type, ItemBehavior behavior, int legacyId) throws RegistryException {
        registerVanilla(type, null, behavior, legacyId);
    }

    private synchronized void registerVanilla(ItemType type, ItemSerializer serializer, int legacyId) throws RegistryException {
        registerVanilla(type, serializer, NoopItemBehavior.INSTANCE, legacyId);
    }

    private synchronized void registerVanilla(ItemType type, ItemSerializer serializer, ItemBehavior behavior, int legacyId) throws RegistryException {
        Objects.requireNonNull(behavior, "type");
        Objects.requireNonNull(behavior, "behavior");
        checkClosed();

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        this.registerType(type, type.getId(), legacyId);
        this.behaviorMap.put(type, behavior);
    }

    public ItemSerializer getSerializer(ItemType type) {
        return serializers.getOrDefault(type, DefaultItemSerializer.INSTANCE);
    }

    public ItemDataSerializer<?> getSerializer(Class<?> metaClass) {
        return dataSerializers.get(metaClass);
    }

    public ItemType getType(Identifier id) {
        return ItemTypes.byId(id);
    }

    public ItemType getType(int legacyId) {
        return getType(getIdentifier(legacyId));
    }

    @Override
    public ItemStack getItem(BlockState state, int amount) throws RegistryException {
        Preconditions.checkNotNull(state);
        Preconditions.checkArgument(amount > 0, "amount must be positive");

        val builder = new CloudItemStackBuilder()
                .blockState(state)
                .amount(amount);

        return builder.build();
    }

    @Override
    public CloudItemStack getItem(ItemType type, int amount, Object... metadata) throws RegistryException {
        Objects.requireNonNull(type, "identifier");
        Preconditions.checkArgument(amount > 0, "amount must be positive");

        val builder = new CloudItemStackBuilder()
                .itemType(type)
                .itemData(metadata);

        return builder.build();
    }

    public Collection<Identifier> getIdentifiers(ItemType type) {
        return this.typeMap.entrySet().stream().filter((e) -> e.getValue() == type).map(Entry::getKey).collect(Collectors.toSet());
    }

    public ItemBehavior getBehavior(ItemType type) {
        return behaviorMap.getOrDefault(type, NoopItemBehavior.INSTANCE);
    }

    public Identifier fromLegacy(int legacyId) throws RegistryException {
        return getIdentifier(legacyId);
    }

    @Override
    public Identifier getIdentifier(int runtimeId) throws RegistryException {
        Identifier identifier = null;
        if (runtimeId < 255) {
            if (runtimeId < 0) {
                runtimeId = 255 - runtimeId;
            }
            try {
                identifier = this.blockRegistry.getNameFromLegacyId(runtimeId);
            } catch (RegistryException e) {
                // ignore
            }
        } else {
            identifier = runtimeIdMap.get(runtimeId);
        }
        if (identifier == null) {
            throw new RegistryException("Runtime ID " + runtimeId + " does not exist");
        }
        return identifier;
    }

    public int getRuntimeId(Identifier identifier) throws RegistryException {
        int runtimeId = runtimeIdMap.inverse().getOrDefault(identifier, Integer.MAX_VALUE);
        if (runtimeId == Integer.MAX_VALUE) {
            try {
                int blockId = this.blockRegistry.getLegacyId(identifier);
                if (blockId > 255) {
                    blockId = 255 - blockId;
                }
                return blockId;
            } catch (RegistryException e) {
                throw new RegistryException(identifier + " is not of a registered item");
            }
        }
        return runtimeId;
    }

    @Override
    public ImmutableList<Identifier> getItems() {
        return ImmutableList.copyOf(this.runtimeIdMap.values());
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();
        this.closed = true;

        List<StartGamePacket.ItemEntry> itemEntries = new ArrayList<>();

        for (ItemData data : VANILLA_ITEMS) {
            itemEntries.add(new StartGamePacket.ItemEntry(data.name, (short) data.id));
        }

//        List<Identifier> customBlocks = this.blockRegistry.getCustomBlocks(); //TODO: custom blocks
//        for (Identifier blockId : customBlocks) {
//            itemEntries.add(new StartGamePacket.ItemEntry(blockId.toString(), (short) this.getRuntimeId(blockId)));
//        }

        int startId = this.lastLegacyId + 1;
        int size = this.runtimeIdAllocator.get();
        for (int id = startId; id < size; id++) {
            Identifier identifier = this.getIdentifier(id);
            itemEntries.add(new StartGamePacket.ItemEntry(identifier.toString(), (short) id));
        }

        this.itemEntries = Collections.unmodifiableList(itemEntries);
    }

    private void checkClosed() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registration is closed");
        }
    }

    public List<StartGamePacket.ItemEntry> getItemEntries() {
        return itemEntries;
    }

    private void registerVanillaItems() throws RegistryException {
        registerVanilla(ItemTypes.IRON_SHOVEL, 256);
        registerVanilla(ItemTypes.IRON_PICKAXE, 257);
        registerVanilla(ItemTypes.IRON_AXE, 258);
        registerVanilla(ItemTypes.FLINT_AND_STEEL, new ItemFlintSteelBehavior(), 259);
        registerVanilla(ItemTypes.APPLE, 260);
        registerVanilla(ItemTypes.BOW, 261);
        registerVanilla(ItemTypes.ARROW, 262);
        registerVanilla(ItemTypes.COAL, 263);
        registerVanilla(ItemTypes.DIAMOND, 264);
        registerVanilla(ItemTypes.IRON_INGOT, 265);
        registerVanilla(ItemTypes.GOLD_INGOT, 266);
        registerVanilla(ItemTypes.IRON_SWORD, 267);
        registerVanilla(ItemTypes.WOODEN_SWORD, 268);
        registerVanilla(ItemTypes.WOODEN_SHOVEL, 269);
        registerVanilla(ItemTypes.WOODEN_PICKAXE, 270);
        registerVanilla(ItemTypes.WOODEN_AXE, 271);
        registerVanilla(ItemTypes.STONE_SWORD, 272);
        registerVanilla(ItemTypes.STONE_SHOVEL, 273);
        registerVanilla(ItemTypes.STONE_PICKAXE, 274);
        registerVanilla(ItemTypes.STONE_AXE, 275);
        registerVanilla(ItemTypes.DIAMOND_SWORD, 276);
        registerVanilla(ItemTypes.DIAMOND_SHOVEL, 277);
        registerVanilla(ItemTypes.DIAMOND_PICKAXE, 278);
        registerVanilla(ItemTypes.DIAMOND_AXE, 279);
        registerVanilla(ItemTypes.STICK, 280);
        registerVanilla(ItemTypes.BOWL, 281);
        registerVanilla(ItemTypes.MUSHROOM_STEW, 282);
        registerVanilla(ItemTypes.GOLDEN_SWORD, 283);
        registerVanilla(ItemTypes.GOLDEN_SHOVEL, 284);
        registerVanilla(ItemTypes.GOLDEN_PICKAXE, 285);
        registerVanilla(ItemTypes.GOLDEN_AXE, 286);
        registerVanilla(ItemTypes.STRING, 287);
        registerVanilla(ItemTypes.FEATHER, 288);
        registerVanilla(ItemTypes.GUNPOWDER, 289);
        registerVanilla(ItemTypes.WOODEN_HOE, 290);
        registerVanilla(ItemTypes.STONE_HOE, 291);
        registerVanilla(ItemTypes.IRON_HOE, 292);
        registerVanilla(ItemTypes.DIAMOND_HOE, 293);
        registerVanilla(ItemTypes.GOLDEN_HOE, 294);
        registerVanilla(ItemTypes.WHEAT_SEEDS, 295);
        registerVanilla(ItemTypes.WHEAT, 296);
        registerVanilla(ItemTypes.BREAD, 297);
        registerVanilla(ItemTypes.LEATHER_HELMET, 298);
        registerVanilla(ItemTypes.LEATHER_CHESTPLATE, 299);
        registerVanilla(ItemTypes.LEATHER_LEGGINGS, 300);
        registerVanilla(ItemTypes.LEATHER_BOOTS, 301);
        registerVanilla(ItemTypes.CHAINMAIL_HELMET, 302);
        registerVanilla(ItemTypes.CHAINMAIL_CHESTPLATE, 303);
        registerVanilla(ItemTypes.CHAINMAIL_LEGGINGS, 304);
        registerVanilla(ItemTypes.CHAINMAIL_BOOTS, 305);
        registerVanilla(ItemTypes.IRON_HELMET, 306);
        registerVanilla(ItemTypes.IRON_CHESTPLATE, 307);
        registerVanilla(ItemTypes.IRON_LEGGINGS, 308);
        registerVanilla(ItemTypes.IRON_BOOTS, 309);
        registerVanilla(ItemTypes.DIAMOND_HELMET, 310);
        registerVanilla(ItemTypes.DIAMOND_CHESTPLATE, 311);
        registerVanilla(ItemTypes.DIAMOND_LEGGINGS, 312);
        registerVanilla(ItemTypes.DIAMOND_BOOTS, 313);
        registerVanilla(ItemTypes.GOLDEN_HELMET, 314);
        registerVanilla(ItemTypes.GOLDEN_CHESTPLATE, 315);
        registerVanilla(ItemTypes.GOLDEN_LEGGINGS, 316);
        registerVanilla(ItemTypes.GOLDEN_BOOTS, 317);
        registerVanilla(ItemTypes.FLINT, 318);
        registerVanilla(ItemTypes.PORKCHOP, 319);
        registerVanilla(ItemTypes.COOKED_PORKCHOP, 320);
        registerVanilla(ItemTypes.PAINTING, new ItemPaintingBehavior(), 321);
        registerVanilla(ItemTypes.GOLDEN_APPLE, new ItemAppleGoldBehavior(), 322);
        registerVanilla(ItemTypes.SIGN, TreeSpeciesSerializer.SIGN, 323);
        registerVanilla(ItemTypes.WOODEN_DOOR, TreeSpeciesSerializer.DOOR, 324);
        registerVanilla(ItemTypes.BUCKET, new ItemBucketBehavior(), 325);

        registerVanilla(ItemTypes.MINECART, new ItemMinecartBehavior(EntityTypes.MINECART), 328);
        registerVanilla(ItemTypes.SADDLE, 329);
        registerVanilla(ItemTypes.IRON_DOOR, 330);
        registerVanilla(ItemTypes.REDSTONE, 331);
        registerVanilla(ItemTypes.SNOWBALL, new ItemProjectileBehavior(EntityTypes.SNOWBALL, 1.5f), 332);
        registerVanilla(ItemTypes.BOAT, new ItemBoatBehavior(), 333);
        registerVanilla(ItemTypes.LEATHER, 334);
        registerVanilla(ItemTypes.KELP, 335);
        registerVanilla(ItemTypes.BRICK, 336);
        registerVanilla(ItemTypes.CLAY_BALL, 337);
        registerVanilla(ItemTypes.REEDS, 338);
        registerVanilla(ItemTypes.PAPER, 339);
        registerVanilla(ItemTypes.BOOK, 340);
        registerVanilla(ItemTypes.SLIME_BALL, 341);
        registerVanilla(ItemTypes.CHEST_MINECART, new ItemMinecartBehavior(EntityTypes.CHEST_MINECART), 342);

        registerVanilla(ItemTypes.EGG, new ItemProjectileBehavior(EntityTypes.EGG, 1.5f), 344);
        registerVanilla(ItemTypes.COMPASS, 345);
        registerVanilla(ItemTypes.FISHING_ROD, 346);
        registerVanilla(ItemTypes.CLOCK, 347);
        registerVanilla(ItemTypes.GLOWSTONE_DUST, 348);
        registerVanilla(ItemTypes.FISH, 349);
        registerVanilla(ItemTypes.COOKED_FISH, 350);
        registerVanilla(ItemTypes.DYE, 351);
        registerVanilla(ItemTypes.BONE, 352);
        registerVanilla(ItemTypes.SUGAR, 353);
        registerVanilla(ItemTypes.CAKE, 354);
        registerVanilla(ItemTypes.BED, 355);
        registerVanilla(ItemTypes.REPEATER, 356);
        registerVanilla(ItemTypes.COOKIE, 357);
        registerVanilla(ItemTypes.MAP, new ItemMapBehavior(), 358);
        registerVanilla(ItemTypes.SHEARS, 359);
        registerVanilla(ItemTypes.MELON, 360);
        registerVanilla(ItemTypes.PUMPKIN_SEEDS, 361);
        registerVanilla(ItemTypes.MELON_SEEDS, 362);
        registerVanilla(ItemTypes.BEEF, 363);
        registerVanilla(ItemTypes.COOKED_BEEF, 364);
        registerVanilla(ItemTypes.CHICKEN, 365);
        registerVanilla(ItemTypes.COOKED_CHICKEN, 366);
        registerVanilla(ItemTypes.ROTTEN_FLESH, 367);
        registerVanilla(ItemTypes.ENDER_PEARL, new ItemProjectileBehavior(EntityTypes.ENDER_PEARL, 1.5f), 368);
        registerVanilla(ItemTypes.BLAZE_ROD, 369);
        registerVanilla(ItemTypes.GHAST_TEAR, 370);
        registerVanilla(ItemTypes.GOLD_NUGGET, 371);
        registerVanilla(ItemTypes.NETHER_WART, 372);
        registerVanilla(ItemTypes.POTION, new ItemPotionBehavior(), 373);
        registerVanilla(ItemTypes.GLASS_BOTTLE, new ItemGlassBottleBehavior(), 374);
        registerVanilla(ItemTypes.SPIDER_EYE, 375);
        registerVanilla(ItemTypes.FERMENTED_SPIDER_EYE, 376);
        registerVanilla(ItemTypes.BLAZE_POWDER, 377);
        registerVanilla(ItemTypes.MAGMA_CREAM, 378);
        registerVanilla(ItemTypes.BREWING_STAND, 379);
        registerVanilla(ItemTypes.CAULDRON, 380);
        registerVanilla(ItemTypes.ENDER_EYE, 381);
        registerVanilla(ItemTypes.SPECKLED_MELON, 382);
        registerVanilla(ItemTypes.SPAWN_EGG, new ItemSpawnEggBehavior(), 383);
        registerVanilla(ItemTypes.EXPERIENCE_BOTTLE, new ItemProjectileBehavior(EntityTypes.XP_BOTTLE, 1f), 384);
        registerVanilla(ItemTypes.FIREBALL, new ItemFireChargeBehavior(), 385);
        registerVanilla(ItemTypes.WRITABLE_BOOK, 386);
        registerVanilla(ItemTypes.WRITTEN_BOOK, 387);
        registerVanilla(ItemTypes.EMERALD, 388);
        registerVanilla(ItemTypes.FRAME, 389);
        registerVanilla(ItemTypes.FLOWER_POT, 390);
        registerVanilla(ItemTypes.CARROT, 391);
        registerVanilla(ItemTypes.POTATO, 392);
        registerVanilla(ItemTypes.BAKED_POTATO, 393);
        registerVanilla(ItemTypes.POISONOUS_POTATO, 394);
        registerVanilla(ItemTypes.EMPTY_MAP, 395);
        registerVanilla(ItemTypes.GOLDEN_CARROT, 396);
        registerVanilla(ItemTypes.SKULL, 397);
        registerVanilla(ItemTypes.CARROT_ON_A_STICK, 398);
        registerVanilla(ItemTypes.NETHER_STAR, 399);
        registerVanilla(ItemTypes.PUMPKIN_PIE, 400);
        registerVanilla(ItemTypes.FIREWORKS, new ItemFireworkBehavior(), 401);

        registerVanilla(ItemTypes.ENCHANTED_BOOK, 403);
        registerVanilla(ItemTypes.COMPARATOR, 404);
        registerVanilla(ItemTypes.NETHERBRICK, 405);
        registerVanilla(ItemTypes.QUARTZ, 406);
        registerVanilla(ItemTypes.TNT_MINECART, new ItemMinecartBehavior(EntityTypes.TNT_MINECART), 407);
        registerVanilla(ItemTypes.HOPPER_MINECART, new ItemMinecartBehavior(EntityTypes.HOPPER_MINECART), 408);
        registerVanilla(ItemTypes.PRISMARINE_SHARD, 409);
        registerVanilla(ItemTypes.HOPPER, 410);
        registerVanilla(ItemTypes.RABBIT, 411);
        registerVanilla(ItemTypes.COOKED_RABBIT, 412);
        registerVanilla(ItemTypes.RABBIT_STEW, 413);
        registerVanilla(ItemTypes.RABBIT_FOOT, 414);
        registerVanilla(ItemTypes.RABBIT_HIDE, 415);
        registerVanilla(ItemTypes.HORSE_ARMOR_LEATHER, 416);
        registerVanilla(ItemTypes.HORSE_ARMOR_IRON, 417);
        registerVanilla(ItemTypes.HORSE_ARMOR_GOLD, 418);
        registerVanilla(ItemTypes.HORSE_ARMOR_DIAMOND, 419);
        registerVanilla(ItemTypes.LEAD, 420);
        registerVanilla(ItemTypes.NAME_TAG, 421);
        registerVanilla(ItemTypes.PRISMARINE_CRYSTALS, 422);
        registerVanilla(ItemTypes.MUTTON_RAW, 423);
        registerVanilla(ItemTypes.MUTTON_COOKED, 424);

        registerVanilla(ItemTypes.ARMOR_STAND, 425);
        registerVanilla(ItemTypes.END_CRYSTAL, new ItemEndCrystalBehavior(), 426);
        registerVanilla(ItemTypes.CHORUS_FRUIT, new ItemChorusFruitBehavior(), 432);
        registerVanilla(ItemTypes.CHORUS_FRUIT_POPPED, 433);

        registerVanilla(ItemTypes.DRAGON_BREATH, 437);
        registerVanilla(ItemTypes.SPLASH_POTION, new ItemPotionSplashBehavior(), 438);

        registerVanilla(ItemTypes.LINGERING_POTION, new ItemPotionLingeringBehavior(), 441);

        registerVanilla(ItemTypes.COMMAND_BLOCK_MINECART, new ItemMinecartBehavior(EntityTypes.COMMAND_BLOCK_MINECART), 444);
        registerVanilla(ItemTypes.ELYTRA, 444);

        registerVanilla(ItemTypes.SHULKER_SHELL, 445);
        registerVanilla(ItemTypes.BANNER, new org.cloudburstmc.server.item.serializer.BannerSerializer(), 446);

        registerVanilla(ItemTypes.IRON_NUGGET, 452);
        registerVanilla(ItemTypes.TRIDENT, new ItemTridentBehavior(), 455);

        registerVanilla(ItemTypes.BEETROOT, 457);
        registerVanilla(ItemTypes.BEETROOT_SEEDS, 458);
        registerVanilla(ItemTypes.BEETROOT_SOUP, 459);
        registerVanilla(ItemTypes.SALMON, 460);
        registerVanilla(ItemTypes.CLOWNFISH, 461);
        registerVanilla(ItemTypes.PUFFERFISH, 462);
        registerVanilla(ItemTypes.COOKED_SALMON, 463);
        registerVanilla(ItemTypes.DRIED_KELP, 464);

        registerVanilla(ItemTypes.APPLE_ENCHANTED, new ItemAppleGoldEnchantedBehavior(), 466);

        registerVanilla(ItemTypes.TURTLE_HELMET, 469);
        registerVanilla(ItemTypes.SWEET_BERRIES, 477);

        registerVanilla(ItemTypes.RECORD, new RecordSerializer(), 500);
//        registerVanilla(ItemTypes.RECORD_CAT, RecordItem.factory("record.cat"), 500);
//        registerVanilla(ItemTypes.RECORD_13, RecordItem.factory("record.13"), 501);
//        registerVanilla(ItemTypes.RECORD_BLOCKS, RecordItem.factory("record.blocks"), 502);
//        registerVanilla(ItemTypes.RECORD_CHIRP, RecordItem.factory("record.chirp"), 503);
//        registerVanilla(ItemTypes.RECORD_FAR, RecordItem.factory("record.far"), 504);
//        registerVanilla(ItemTypes.RECORD_MALL, RecordItem.factory("record.mall"), 505);
//        registerVanilla(ItemTypes.RECORD_MELLOHI, RecordItem.factory("record.mellohi"), 506);
//        registerVanilla(ItemTypes.RECORD_STAL, RecordItem.factory("record.stal"), 507);
//        registerVanilla(ItemTypes.RECORD_STRAD, RecordItem.factory("record.strad"), 508);
//        registerVanilla(ItemTypes.RECORD_WARD, RecordItem.factory("record.ward"), 509);
//        registerVanilla(ItemTypes.RECORD_11, RecordItem.factory("record.11"), 510);
//        registerVanilla(ItemTypes.RECORD_WAIT, RecordItem.factory("record.wait"), 511);

        registerVanilla(ItemTypes.SHIELD, 513);

        registerVanilla(ItemTypes.CAMPFIRE, 720);

        registerVanilla(ItemTypes.HONEYCOMB, 736);
        registerVanilla(ItemTypes.HONEY_BOTTLE, 737);

        registerVanilla(ItemTypes.NETHERITE_SWORD, 743);
        registerVanilla(ItemTypes.NETHERITE_SHOVEL, 744);
        registerVanilla(ItemTypes.NETHERITE_PICKAXE, 745);
        registerVanilla(ItemTypes.NETHERITE_AXE, 746);
        registerVanilla(ItemTypes.NETHERITE_HOE, 747);
        registerVanilla(ItemTypes.NETHERITE_HELMET, 748);
        registerVanilla(ItemTypes.NETHERITE_CHESTPLATE, 749);
        registerVanilla(ItemTypes.NETHERITE_LEGGINGS, 750);
        registerVanilla(ItemTypes.NETHERITE_BOOTS, 751);
    }

    private void registerType(ItemType type, Identifier id, int legacyId) {
        this.typeMap.put(id, type);
        this.runtimeIdMap.put(legacyId, id);

        if (this.lastLegacyId < legacyId) {
            this.lastLegacyId = legacyId;
            this.runtimeIdAllocator.set(this.lastLegacyId + 1);
        }
    }

    private void registerType(ItemType type, Identifier... identifiers) {
        for (Identifier id : identifiers) {
            this.typeMap.put(id, type);
        }
    }

    /**
     * register additional vanilla identifiers not used as a separate ItemType
     */
    private void registerVanillaIdentifiers() {
        registerType(ItemTypes.SIGN, ItemIds.SPRUCE_SIGN, 472);
        registerType(ItemTypes.SIGN, ItemIds.BIRCH_SIGN, 473);
        registerType(ItemTypes.SIGN, ItemIds.JUNGLE_SIGN, 474);
        registerType(ItemTypes.SIGN, ItemIds.ACACIA_SIGN, 475);
        registerType(ItemTypes.SIGN, ItemIds.DARK_OAK_SIGN, 476);
        registerType(ItemTypes.SIGN, ItemIds.CRIMSON_SIGN, 753);
        registerType(ItemTypes.SIGN, ItemIds.WARPED_SIGN, 754);

        registerType(ItemTypes.WOODEN_DOOR, ItemIds.SPRUCE_DOOR, 427);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.BIRCH_DOOR, 428);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.JUNGLE_DOOR, 429);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.ACACIA_DOOR, 430);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.DARK_OAK_DOOR, 431);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.CRIMSON_DOOR, 755);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.WARPED_DOOR, 756);

        registerType(ItemTypes.RECORD, ItemIds.RECORD_BLOCKS, 502);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_CHIRP, 503);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_FAR, 504);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_MALL, 505);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_MELLOHI, 506);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_STAL, 507);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_STRAD, 508);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_WARD, 509);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_11, 510);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_WAIT, 511);
    }

    private void registerVanillaDataSerializers() {
        this.registerDataSerializer(Banner.class, new BannerSerializer());
        this.registerDataSerializer(Damageable.class, new DamageableSerializer());
        this.registerDataSerializer(Firework.class, new FireworkSerializer());
        this.registerDataSerializer(MapItem.class, new MapSerializer());
        this.registerDataSerializer(WrittenBook.class, new WrittenBookSerializer());
        this.registerDataSerializer(EntityType.class, new EntityTypeSerializer());
        this.registerDataSerializer(Potion.class, new PotionSerializer());
    }

    public void registerCreativeItem(ItemStack item) {
        Preconditions.checkNotNull(item, "item");
        this.creativeItems.add(item);
    }

    public List<ItemStack> getCreativeItems() {
        return ImmutableList.copyOf(creativeItems);
    }

    public CreativeContentPacket getCreativeContent() {
        if (creativeContent == null) {
            synchronized (this.creativeItems) {
                if (creativeContent == null) {
                    CreativeContentPacket pk = new CreativeContentPacket();

                    val contents = ItemUtils.toNetwork(this.creativeItems);

                    for (int i = 0; i < contents.length; i++) {
                        contents[i].setNetId(i + 1);
                    }

                    pk.setContents(contents);
                    creativeContent = pk;
                }
            }
        }

        return creativeContent;
    }

    @SuppressWarnings("rawtypes")
    private void registerVanillaCreativeItems() {
        Config config = new Config(Config.JSON);
        config.load(Server.class.getClassLoader().getResourceAsStream("data/creative_items.json"));
        List<Map> list = config.getMapList("items");

        for (Map map : list) {
            try {
//                registerCreativeItem(); //TODO: creative items
//                addCreativeItem(fromJson(map));
            } catch (RegistryException e) {
                // ignore
            } catch (Exception e) {
                log.error("Error whilst adding creative item", e);
            }
        }
    }

    @Getter
    private static class ItemData {
        private String name;
        private int id;
    }
}
