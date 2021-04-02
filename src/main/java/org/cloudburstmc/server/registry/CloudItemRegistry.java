package org.cloudburstmc.server.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.CreativeContentPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.minidev.json.JSONArray;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.item.data.*;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.behavior.*;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
public class CloudItemRegistry implements ItemRegistry {
    private static final CloudItemRegistry INSTANCE;
    private static final BiMap<Identifier, Integer> VANILLA_LEGACY_IDS = HashBiMap.create();

    static {
        try (InputStream in = RegistryUtils.getOrAssertResource("data/runtime_item_states.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (JsonNode item : json) {
                int id = item.get("id").intValue();
                if (id <= 255) continue;
                VANILLA_LEGACY_IDS.put(Identifier.fromString(item.get("name").asText()), id);
            }
        } catch (IOException e) {
            throw new RegistryException("Unable to load legacy IDs", e);
        }
        INSTANCE = new CloudItemRegistry(CloudBlockRegistry.get()); // Needs to be initialized afterwards
    }

    private final Reference2ReferenceMap<Identifier, ItemType> typeMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<Class<?>, ItemDataSerializer<?>> dataSerializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemBehavior> behaviorMap = new Reference2ObjectOpenHashMap<>();
    private final List<ItemStack> creativeItems = new ArrayList<>();
    private final BiMap<Integer, Identifier> runtimeIdMap = HashBiMap.create();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger(256);
    private final CloudBlockRegistry blockRegistry;
    private List<StartGamePacket.ItemEntry> itemEntries;
    private volatile CreativeContentPacket creativeContent;

    public static final CloudItemStack AIR = new CloudItemStack(Identifiers.AIR, BlockTypes.AIR);

    private volatile boolean closed;

    private CloudItemRegistry(CloudBlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
        try {
            this.registerVanillaItems();
            this.registerVanillaIdentifiers();
            this.registerVanillaDataSerializers();

            for (Identifier id : VANILLA_LEGACY_IDS.keySet()) {
                if (!runtimeIdMap.inverse().containsKey(id)) {
                    System.out.println("Unimplemented item found: " + id.getName());
                    registerType(ItemTypes.UNKNOWN, id);
                }
            }
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }

    }

    public static CloudItemRegistry get() {
        return INSTANCE;
    }

    public synchronized <T> void registerDataSerializer(Class<T> metadataClass, ItemDataSerializer<T> serializer) {
        Preconditions.checkNotNull(metadataClass, "metadataClass");
        Preconditions.checkNotNull(serializer, "serializer");
        this.dataSerializers.put(metadataClass, serializer);
    }

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

    private synchronized void registerVanilla(ItemType type /*,int legacyId*/) throws RegistryException {
        registerVanilla(type, null, NoopItemBehavior.INSTANCE /*,legacyId*/);
    }

    private synchronized void registerVanilla(ItemType type, ItemBehavior behavior/*, int legacyId*/) throws RegistryException {
        registerVanilla(type, null, behavior/*, legacyId*/);
    }

    private synchronized void registerVanilla(ItemType type, ItemSerializer serializer/*, int legacyId*/) throws RegistryException {
        registerVanilla(type, serializer, NoopItemBehavior.INSTANCE /*, legacyId*/);
    }

    private synchronized void registerVanilla(ItemType type, ItemSerializer serializer, ItemBehavior behavior /*,int legacyId*/) throws RegistryException {
        Objects.requireNonNull(behavior, "type");
        Objects.requireNonNull(behavior, "behavior");
        checkClosed();

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        this.registerType(type, type.getId()/*, legacyId*/);
        this.behaviorMap.put(type, behavior);
    }

    public ItemSerializer getSerializer(ItemType type) {
        return serializers.getOrDefault(type, DefaultItemSerializer.INSTANCE);
    }

    public ItemDataSerializer<?> getSerializer(Class<?> metaClass) {
        return dataSerializers.get(metaClass);
    }

    public ItemType getType(Identifier id) {
        return this.typeMap.get(id);
    }

    public ItemType getType(int legacyId) {
        return getType(getIdentifier(legacyId));
    }

    public ItemStack getItemLegacy(int legacyId) {
        return getItem(getType(legacyId), 1);
    }

    public ItemStack getItemLegacy(int legacyId, short damage) {
        return getItemLegacy(legacyId, damage, 1);
    }

    public ItemStack getItemLegacy(int legacyId, short damage, int amount) {
        return ItemUtils.deserializeItem(getIdentifier(legacyId), damage, amount, NbtMap.EMPTY);
    }

    @Override
    public void register(ItemType itemType, ItemBehavior itemBehavior, Identifier... identifiers) throws RegistryException {
        this.register(itemType,DefaultItemSerializer.INSTANCE,itemBehavior,identifiers);
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
    public ItemStack getItem(ItemType type, int amount, Object... metadata) throws RegistryException {
        Objects.requireNonNull(type, "identifier");
        Preconditions.checkArgument(amount > 0, "amount must be positive");

        val builder = new CloudItemStackBuilder()
                .itemType(type)
                .amount(amount)
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
        return VANILLA_LEGACY_IDS.inverse().get(legacyId);
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
        this.registerVanillaCreativeItems();

        List<StartGamePacket.ItemEntry> itemEntries = new ArrayList<>();

        for(int runtimeid : runtimeIdMap.keySet()) {
            itemEntries.add(new StartGamePacket.ItemEntry(runtimeIdMap.get(runtimeid).toString(), (short) runtimeid));
        }

//        List<Identifier> customBlocks = this.blockRegistry.getCustomBlocks(); //TODO: custom blocks
//        for (Identifier blockId : customBlocks) {
//            itemEntries.add(new StartGamePacket.ItemEntry(blockId.toString(), (short) this.getRuntimeId(blockId)));
//        }

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
        registerVanilla(ItemTypes.IRON_SHOVEL);
        registerVanilla(ItemTypes.IRON_PICKAXE);
        registerVanilla(ItemTypes.IRON_AXE);
        registerVanilla(ItemTypes.FLINT_AND_STEEL, new ItemFlintSteelBehavior());
        registerVanilla(ItemTypes.APPLE);
        registerVanilla(ItemTypes.BOW);
        registerVanilla(ItemTypes.ARROW);
        registerVanilla(ItemTypes.COAL);
        registerVanilla(ItemTypes.DIAMOND);
        registerVanilla(ItemTypes.IRON_INGOT);
        registerVanilla(ItemTypes.GOLD_INGOT);
        registerVanilla(ItemTypes.IRON_SWORD);
        registerVanilla(ItemTypes.WOODEN_SWORD);
        registerVanilla(ItemTypes.WOODEN_SHOVEL);
        registerVanilla(ItemTypes.WOODEN_PICKAXE);
        registerVanilla(ItemTypes.WOODEN_AXE);
        registerVanilla(ItemTypes.STONE_SWORD);
        registerVanilla(ItemTypes.STONE_SHOVEL);
        registerVanilla(ItemTypes.STONE_PICKAXE);
        registerVanilla(ItemTypes.STONE_AXE);
        registerVanilla(ItemTypes.DIAMOND_SWORD);
        registerVanilla(ItemTypes.DIAMOND_SHOVEL);
        registerVanilla(ItemTypes.DIAMOND_PICKAXE);
        registerVanilla(ItemTypes.DIAMOND_AXE);
        registerVanilla(ItemTypes.STICK);
        registerVanilla(ItemTypes.BOWL);
        registerVanilla(ItemTypes.MUSHROOM_STEW);
        registerVanilla(ItemTypes.GOLDEN_SWORD);
        registerVanilla(ItemTypes.GOLDEN_SHOVEL);
        registerVanilla(ItemTypes.GOLDEN_PICKAXE);
        registerVanilla(ItemTypes.GOLDEN_AXE);
        registerVanilla(ItemTypes.STRING);
        registerVanilla(ItemTypes.FEATHER);
        registerVanilla(ItemTypes.GUNPOWDER);
        registerVanilla(ItemTypes.WOODEN_HOE);
        registerVanilla(ItemTypes.STONE_HOE);
        registerVanilla(ItemTypes.IRON_HOE);
        registerVanilla(ItemTypes.DIAMOND_HOE);
        registerVanilla(ItemTypes.GOLDEN_HOE);
        registerVanilla(ItemTypes.WHEAT_SEEDS);
        registerVanilla(ItemTypes.WHEAT);
        registerVanilla(ItemTypes.BREAD);
        registerVanilla(ItemTypes.LEATHER_HELMET);
        registerVanilla(ItemTypes.LEATHER_CHESTPLATE);
        registerVanilla(ItemTypes.LEATHER_LEGGINGS);
        registerVanilla(ItemTypes.LEATHER_BOOTS);
        registerVanilla(ItemTypes.CHAINMAIL_HELMET);
        registerVanilla(ItemTypes.CHAINMAIL_CHESTPLATE);
        registerVanilla(ItemTypes.CHAINMAIL_LEGGINGS);
        registerVanilla(ItemTypes.CHAINMAIL_BOOTS);
        registerVanilla(ItemTypes.IRON_HELMET);
        registerVanilla(ItemTypes.IRON_CHESTPLATE);
        registerVanilla(ItemTypes.IRON_LEGGINGS);
        registerVanilla(ItemTypes.IRON_BOOTS);
        registerVanilla(ItemTypes.DIAMOND_HELMET);
        registerVanilla(ItemTypes.DIAMOND_CHESTPLATE);
        registerVanilla(ItemTypes.DIAMOND_LEGGINGS);
        registerVanilla(ItemTypes.DIAMOND_BOOTS);
        registerVanilla(ItemTypes.GOLDEN_HELMET);
        registerVanilla(ItemTypes.GOLDEN_CHESTPLATE);
        registerVanilla(ItemTypes.GOLDEN_LEGGINGS);
        registerVanilla(ItemTypes.GOLDEN_BOOTS);
        registerVanilla(ItemTypes.FLINT);
        registerVanilla(ItemTypes.PORKCHOP);
        registerVanilla(ItemTypes.COOKED_PORKCHOP);
        registerVanilla(ItemTypes.PAINTING, new ItemPaintingBehavior());
        registerVanilla(ItemTypes.GOLDEN_APPLE, new ItemAppleGoldBehavior());
        registerVanilla(ItemTypes.SIGN, TreeSpeciesSerializer.SIGN);
        registerVanilla(ItemTypes.WOODEN_DOOR, TreeSpeciesSerializer.DOOR);
        registerVanilla(ItemTypes.BUCKET, new ItemBucketBehavior());

        registerVanilla(ItemTypes.MINECART, new ItemMinecartBehavior(EntityTypes.MINECART));
        registerVanilla(ItemTypes.SADDLE);
        registerVanilla(ItemTypes.IRON_DOOR);
        registerVanilla(ItemTypes.REDSTONE);
        registerVanilla(ItemTypes.SNOWBALL, new ItemProjectileBehavior(EntityTypes.SNOWBALL, 1.5f));
        registerVanilla(ItemTypes.BOAT, TreeSpeciesSerializer.BOAT, new ItemBoatBehavior());
        registerVanilla(ItemTypes.LEATHER);
        registerVanilla(ItemTypes.KELP);
        registerVanilla(ItemTypes.BRICK);
        registerVanilla(ItemTypes.CLAY_BALL);
        registerVanilla(ItemTypes.REEDS);
        registerVanilla(ItemTypes.PAPER);
        registerVanilla(ItemTypes.BOOK);
        registerVanilla(ItemTypes.SLIME_BALL);
        registerVanilla(ItemTypes.CHEST_MINECART, new ItemMinecartBehavior(EntityTypes.CHEST_MINECART));

        registerVanilla(ItemTypes.EGG, new ItemProjectileBehavior(EntityTypes.EGG, 1.5f));
        registerVanilla(ItemTypes.COMPASS);
        registerVanilla(ItemTypes.FISHING_ROD);
        registerVanilla(ItemTypes.CLOCK);
        registerVanilla(ItemTypes.GLOWSTONE_DUST);
        registerVanilla(ItemTypes.FISH);
        registerVanilla(ItemTypes.COOKED_FISH);
        registerVanilla(ItemTypes.DYE, new DyeSerializer());
        registerVanilla(ItemTypes.BONE);
        registerVanilla(ItemTypes.SUGAR);
        registerVanilla(ItemTypes.CAKE);
        registerVanilla(ItemTypes.BED, EnumDamageSerializer.DYE_COLOR);
        registerVanilla(ItemTypes.REPEATER);
        registerVanilla(ItemTypes.COOKIE);
        registerVanilla(ItemTypes.MAP, new ItemMapBehavior());
        registerVanilla(ItemTypes.SHEARS);
        registerVanilla(ItemTypes.MELON);
        registerVanilla(ItemTypes.PUMPKIN_SEEDS);
        registerVanilla(ItemTypes.MELON_SEEDS);
        registerVanilla(ItemTypes.BEEF);
        registerVanilla(ItemTypes.COOKED_BEEF);
        registerVanilla(ItemTypes.CHICKEN);
        registerVanilla(ItemTypes.COOKED_CHICKEN);
        registerVanilla(ItemTypes.ROTTEN_FLESH);
        registerVanilla(ItemTypes.ENDER_PEARL, new ItemProjectileBehavior(EntityTypes.ENDER_PEARL, 1.5f));
        registerVanilla(ItemTypes.BLAZE_ROD);
        registerVanilla(ItemTypes.GHAST_TEAR);
        registerVanilla(ItemTypes.GOLD_NUGGET);
        registerVanilla(ItemTypes.NETHER_WART);
        registerVanilla(ItemTypes.POTION, new ItemPotionBehavior());
        registerVanilla(ItemTypes.GLASS_BOTTLE, new ItemGlassBottleBehavior());
        registerVanilla(ItemTypes.SPIDER_EYE);
        registerVanilla(ItemTypes.FERMENTED_SPIDER_EYE);
        registerVanilla(ItemTypes.BLAZE_POWDER);
        registerVanilla(ItemTypes.MAGMA_CREAM);
        registerVanilla(ItemTypes.BREWING_STAND);
        registerVanilla(ItemTypes.CAULDRON);
        registerVanilla(ItemTypes.ENDER_EYE);
        registerVanilla(ItemTypes.SPECKLED_MELON);
        registerVanilla(ItemTypes.SPAWN_EGG, new ItemSpawnEggBehavior());
        registerVanilla(ItemTypes.EXPERIENCE_BOTTLE, new ItemProjectileBehavior(EntityTypes.XP_BOTTLE, 1f));
        registerVanilla(ItemTypes.FIREBALL, new ItemFireChargeBehavior());
        registerVanilla(ItemTypes.WRITABLE_BOOK);
        registerVanilla(ItemTypes.WRITTEN_BOOK);
        registerVanilla(ItemTypes.EMERALD);
        registerVanilla(ItemTypes.FRAME);
        registerVanilla(ItemTypes.FLOWER_POT);
        registerVanilla(ItemTypes.CARROT);
        registerVanilla(ItemTypes.POTATO);
        registerVanilla(ItemTypes.BAKED_POTATO);
        registerVanilla(ItemTypes.POISONOUS_POTATO);
        registerVanilla(ItemTypes.EMPTY_MAP);
        registerVanilla(ItemTypes.GOLDEN_CARROT);
        registerVanilla(ItemTypes.SKULL);
        registerVanilla(ItemTypes.CARROT_ON_A_STICK);
        registerVanilla(ItemTypes.NETHER_STAR);
        registerVanilla(ItemTypes.PUMPKIN_PIE);
        registerVanilla(ItemTypes.FIREWORKS, new ItemFireworkBehavior());

        registerVanilla(ItemTypes.ENCHANTED_BOOK);
        registerVanilla(ItemTypes.COMPARATOR);
        registerVanilla(ItemTypes.NETHERBRICK);
        registerVanilla(ItemTypes.QUARTZ);
        registerVanilla(ItemTypes.TNT_MINECART, new ItemMinecartBehavior(EntityTypes.TNT_MINECART));
        registerVanilla(ItemTypes.HOPPER_MINECART, new ItemMinecartBehavior(EntityTypes.HOPPER_MINECART));
        registerVanilla(ItemTypes.PRISMARINE_SHARD);
        registerVanilla(ItemTypes.HOPPER);
        registerVanilla(ItemTypes.RABBIT);
        registerVanilla(ItemTypes.COOKED_RABBIT);
        registerVanilla(ItemTypes.RABBIT_STEW);
        registerVanilla(ItemTypes.RABBIT_FOOT);
        registerVanilla(ItemTypes.RABBIT_HIDE);
        registerVanilla(ItemTypes.HORSE_ARMOR_LEATHER);
        registerVanilla(ItemTypes.HORSE_ARMOR_IRON);
        registerVanilla(ItemTypes.HORSE_ARMOR_GOLD);
        registerVanilla(ItemTypes.HORSE_ARMOR_DIAMOND);
        registerVanilla(ItemTypes.LEAD);
        registerVanilla(ItemTypes.NAME_TAG);
        registerVanilla(ItemTypes.PRISMARINE_CRYSTALS);
        registerVanilla(ItemTypes.MUTTON_RAW);
        registerVanilla(ItemTypes.MUTTON_COOKED);

        registerVanilla(ItemTypes.ARMOR_STAND);
        registerVanilla(ItemTypes.END_CRYSTAL, new ItemEndCrystalBehavior());
        registerVanilla(ItemTypes.CHORUS_FRUIT, new ItemChorusFruitBehavior());
        registerVanilla(ItemTypes.CHORUS_FRUIT_POPPED);

        registerVanilla(ItemTypes.DRAGON_BREATH);
        registerVanilla(ItemTypes.SPLASH_POTION, new ItemPotionSplashBehavior());

        registerVanilla(ItemTypes.LINGERING_POTION, new ItemPotionLingeringBehavior());

        registerVanilla(ItemTypes.COMMAND_BLOCK_MINECART, new ItemMinecartBehavior(EntityTypes.COMMAND_BLOCK_MINECART));
        registerVanilla(ItemTypes.ELYTRA);

        registerVanilla(ItemTypes.SHULKER_SHELL);
        registerVanilla(ItemTypes.BANNER, new org.cloudburstmc.server.item.serializer.BannerSerializer());
        registerVanilla(ItemTypes.BANNER_PATTERN);

        registerVanilla(ItemTypes.IRON_NUGGET);
        registerVanilla(ItemTypes.NAUTILUS_SHELL);
        registerVanilla(ItemTypes.TRIDENT, new ItemTridentBehavior());

        registerVanilla(ItemTypes.BEETROOT);
        registerVanilla(ItemTypes.BEETROOT_SEEDS);
        registerVanilla(ItemTypes.BEETROOT_SOUP);
        registerVanilla(ItemTypes.SALMON);
        registerVanilla(ItemTypes.CLOWNFISH);
        registerVanilla(ItemTypes.PUFFERFISH);
        registerVanilla(ItemTypes.COOKED_SALMON);
        registerVanilla(ItemTypes.DRIED_KELP);

        registerVanilla(ItemTypes.APPLE_ENCHANTED, new ItemAppleGoldEnchantedBehavior());

        registerVanilla(ItemTypes.TURTLE_HELMET);
        registerVanilla(ItemTypes.SWEET_BERRIES);

        registerVanilla(ItemTypes.RECORD, new RecordSerializer());

        registerVanilla(ItemTypes.SHIELD);

        registerVanilla(ItemTypes.CAMPFIRE);

        registerVanilla(ItemTypes.HONEYCOMB);
        registerVanilla(ItemTypes.HONEY_BOTTLE);

        registerVanilla(ItemTypes.NETHERITE_SWORD);
        registerVanilla(ItemTypes.NETHERITE_SHOVEL);
        registerVanilla(ItemTypes.NETHERITE_PICKAXE);
        registerVanilla(ItemTypes.NETHERITE_AXE);
        registerVanilla(ItemTypes.NETHERITE_HOE);
        registerVanilla(ItemTypes.NETHERITE_HELMET);
        registerVanilla(ItemTypes.NETHERITE_CHESTPLATE);
        registerVanilla(ItemTypes.NETHERITE_LEGGINGS);
        registerVanilla(ItemTypes.NETHERITE_BOOTS);

        registerVanilla(ItemTypes.UNKNOWN);
    }

    private void registerType(ItemType type, Identifier id/*, int legacyId*/) {
        this.typeMap.put(id, type);
        ItemTypes.addType(id, type);
        int runtime = this.runtimeIdAllocator.getAndIncrement();
        this.runtimeIdMap.put(runtime, id);


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
        registerType(ItemTypes.SIGN, ItemIds.SPRUCE_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.BIRCH_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.JUNGLE_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.ACACIA_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.DARK_OAK_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.CRIMSON_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.WARPED_SIGN);

        registerType(ItemTypes.WOODEN_DOOR, ItemIds.SPRUCE_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.BIRCH_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.JUNGLE_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.ACACIA_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.DARK_OAK_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.CRIMSON_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.WARPED_DOOR);

        registerType(ItemTypes.RECORD, ItemIds.RECORD_BLOCKS);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_CHIRP);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_FAR);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_MALL);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_MELLOHI);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_STAL);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_STRAD);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_WARD);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_11);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_WAIT);

        registerType(ItemTypes.COAL, ItemIds.CHARCOAL);

        registerType(ItemTypes.DYE, ItemIds.RED_DYE);
        registerType(ItemTypes.DYE, ItemIds.GREEN_DYE);
        registerType(ItemTypes.DYE, ItemIds.COCOA_BEANS);
        registerType(ItemTypes.DYE, ItemIds.LAPIS_LAZULI);
        registerType(ItemTypes.DYE, ItemIds.PURPLE_DYE);
        registerType(ItemTypes.DYE, ItemIds.CYAN_DYE);
        registerType(ItemTypes.DYE, ItemIds.LIGHT_GRAY_DYE);
        registerType(ItemTypes.DYE, ItemIds.GRAY_DYE);
        registerType(ItemTypes.DYE, ItemIds.PINK_DYE);
        registerType(ItemTypes.DYE, ItemIds.LIME_DYE);
        registerType(ItemTypes.DYE, ItemIds.YELLOW_DYE);
        registerType(ItemTypes.DYE, ItemIds.LIGHT_BLUE_DYE);
        registerType(ItemTypes.DYE, ItemIds.MAGENTA_DYE);
        registerType(ItemTypes.DYE, ItemIds.ORANGE_DYE);
        registerType(ItemTypes.DYE, ItemIds.BONE_MEAL);
        registerType(ItemTypes.DYE, ItemIds.BLACK_DYE);
        registerType(ItemTypes.DYE, ItemIds.BROWN_DYE);
        registerType(ItemTypes.DYE, ItemIds.WHITE_DYE);
        registerType(ItemTypes.DYE, ItemIds.BLUE_DYE);

        registerType(ItemTypes.BOAT, ItemIds.OAK_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.SPRUCE_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.BIRCH_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.JUNGLE_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.ACACIA_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.DARK_OAK_BOAT);

        registerType(ItemTypes.BANNER_PATTERN, ItemIds.CREEPER_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.SKULL_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.FLOWER_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.MOJANG_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.FIELD_MASONED_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.BORDURE_INDENTED_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.PIGLIN_BANNER_PATTERN);

        registerType(ItemTypes.SPAWN_EGG, ItemIds.CHICKEN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.BEE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.COW_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PIG_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SHEEP_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WOLF_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.POLAR_BEAR_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.OCELOT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.CAT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.MOOSHROOM_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.BAT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PARROT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.RABBIT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.LLAMA_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.HORSE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.DONKEY_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.MULE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SKELETON_HORSE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_HORSE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.TROPICAL_FISH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.COD_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PUFFERFISH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SALMON_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.DOLPHIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.TURTLE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PANDA_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.FOX_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.CREEPER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ENDERMAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SILVERFISH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SKELETON_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WITHER_SKELETON_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.STRAY_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SLIME_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SPIDER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_PIGMAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.HUSK_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.DROWNED_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SQUID_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.CAVE_SPIDER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WITCH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.GUARDIAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ELDER_GUARDIAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ENDERMITE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.MAGMA_CUBE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.STRIDER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.HOGLIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PIGLIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOGLIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PIGLIN_BRUTE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.GHAST_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.BLAZE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SHULKER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.VINDICATOR_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.EVOKER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.VEX_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.VILLAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WANDERING_TRADER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_VILLAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PHANTOM_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PILLAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.RAVAGER_SPAWN_EGG);

        registerType(ItemTypes.BUCKET, ItemIds.MILK_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.COD_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.SALMON_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.TROPICAL_FISH_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.PUFFERFISH_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.WATER_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.LAVA_BUCKET);

    }

    private void registerVanillaDataSerializers() {
        this.registerDataSerializer(BannerData.class, new BannerDataSerializer());
        this.registerDataSerializer(Damageable.class, new DamageableSerializer());
        this.registerDataSerializer(FireworkData.class, new FireworkSerializer());
        this.registerDataSerializer(MapItem.class, new MapSerializer());
        this.registerDataSerializer(WrittenBook.class, new WrittenBookSerializer());
        this.registerDataSerializer(EntityType.class, new EntityTypeSerializer());
        this.registerDataSerializer(Potion.class, new PotionSerializer());
        this.registerDataSerializer(Bucket.class, BucketSerializer.INSTANCE);
        this.registerDataSerializer(Coal.class, EnumSerializer.COAL);
    }

    @SuppressWarnings({"unchecked"})
    public synchronized void loadCreativeItems(URI jsonFile) {
        JSONArray json;
        try {
            json = Bootstrap.JSON_MAPPER.readValue(jsonFile.toURL(), new TypeReference<Map<String, JSONArray>>(){}).get("items");
        } catch (IOException e) {
            throw new RegistryException("Unable to load creative items file: " + jsonFile.toString(), e);
        } catch (Exception e) {
            throw new RegistryException("Unknown error when loading creative items file.", e);
        }
        for (Object item : json) {
            ItemStack i = ItemUtils.fromJson(((Map<String, Object>) item));
            this.registerCreativeItem(i);
        }
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

                    val contents = ItemUtils.toNetwork(this.creativeItems).toArray(new com.nukkitx.protocol.bedrock.data.inventory.ItemData[0]);

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

    private void registerVanillaCreativeItems() {
        try {
            URI uri = Thread.currentThread().getContextClassLoader().getResource("data/creative_items.json").toURI();
            this.loadCreativeItems(uri);
        } catch (URISyntaxException e) {
            throw new RegistryException("Unable to load vanilla creative items list.");
        }
    }

    public int getCreativeItemIndex(ItemStack item) {
        for (int i = 0; i < creativeItems.size(); i++) {
            if (item.equals(creativeItems.get(i))) {
                return i;
            }
        }

        return -1;
    }

    @Getter
    private static class ItemData {
        private String name;
        private int id;
    }
}
