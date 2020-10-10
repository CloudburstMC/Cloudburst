package org.cloudburstmc.server.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.item.ItemFactory;
import org.cloudburstmc.server.item.behavior.*;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class ItemRegistry implements Registry {
    private static final ItemRegistry INSTANCE;
    private static final List<ItemData> VANILLA_ITEMS;

    static {
        InputStream stream = RegistryUtils.getOrAssertResource("data/runtime_item_states.json"); //TODO: use legacy_item_ids.json instead

        try {
            VANILLA_ITEMS = Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<List<ItemData>>() {
            });
        } catch (IOException e) {
            throw new AssertionError("Unable to load vanilla items", e);
        }

        INSTANCE = new ItemRegistry(BlockRegistry.get()); // Needs to be initialized afterwards
    }

    private final Map<Identifier, ItemFactory> factoryMap = new IdentityHashMap<>();
    private final BiMap<Integer, Identifier> runtimeIdMap = HashBiMap.create();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger();
    private int lastLegacyId;
    private final BlockRegistry blockRegistry;
    private List<StartGamePacket.ItemEntry> itemEntries;
    private volatile boolean closed;

    private ItemRegistry(BlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
        try {
            this.registerVanillaItems();
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }

        // register missing vanilla items.
        for (ItemData item : VANILLA_ITEMS) {
            if (item.id < 256) {
                continue;
            }
            Identifier identifier = Identifier.fromString(item.name);
            if (!this.factoryMap.containsKey(identifier)) {
                log.debug("Non-implemented item found {}", identifier);
                registerVanilla(identifier, SimpleItem::new, item.id);
            }
        }
    }

    public static ItemRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(Identifier identifier, ItemFactory itemFactory) throws RegistryException {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(itemFactory, "itemFactory");
        checkClosed();
        if (this.factoryMap.putIfAbsent(identifier, itemFactory) != null) {
            throw new RegistryException(identifier + " has already been registered");
        }
        this.runtimeIdMap.put(this.runtimeIdAllocator.getAndIncrement(), identifier);
    }

    private synchronized void registerVanilla(Identifier identifier, ItemFactory itemFactory, int legacyId) throws RegistryException {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(itemFactory, "itemFactory");
        checkClosed();
        this.factoryMap.put(identifier, itemFactory);
        this.runtimeIdMap.put(legacyId, identifier);
        if (this.lastLegacyId < legacyId) {
            this.lastLegacyId = legacyId;
            this.runtimeIdAllocator.set(this.lastLegacyId + 1);
        }
    }

    public Item getItem(BlockState state) throws RegistryException {
        Preconditions.checkNotNull(state);
        return new BlockItem(state);
    }

    public Item getItem(Identifier identifier) throws RegistryException {
        return getItem(identifier, 0);
    }

    public Item getItem(Identifier identifier, int meta) throws RegistryException {
        Objects.requireNonNull(identifier, "identifier");
        ItemFactory itemFactory = this.factoryMap.get(identifier);
        if (itemFactory == null) {
            if (this.blockRegistry.isBlock(identifier)) {
                BlockState state = BlockStateMetaMappings.getStateFromMeta(identifier, meta);
                if (state == null) {
                    throw new RegistryException("Item '" + identifier + "' is not registered");
                }
                return new BlockItem(state);
            } else {
                log.warn("Registering unknown item {}", identifier);
                itemFactory = SimpleItem::new;
                this.factoryMap.put(identifier, itemFactory);
                this.runtimeIdMap.put(this.runtimeIdAllocator.getAndIncrement(), identifier);
            }
        }
        return itemFactory.create(identifier);
    }

    public Identifier fromLegacy(int legacyId) throws RegistryException {
        return getIdentifier(legacyId);
    }

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
        registerVanilla(ItemIds.IRON_SHOVEL, ItemShovelIron::new, 256);
        registerVanilla(ItemIds.IRON_PICKAXE, ItemPickaxeIron::new, 257);
        registerVanilla(ItemIds.IRON_AXE, ItemAxeIron::new, 258);
        registerVanilla(ItemIds.FLINT_AND_STEEL, ItemFlintSteel::new, 259);
        registerVanilla(ItemIds.APPLE, ItemApple::new, 260);
        registerVanilla(ItemIds.BOW, ItemBow::new, 261);
        registerVanilla(ItemIds.ARROW, ItemArrow::new, 262);
        registerVanilla(ItemIds.COAL, SimpleItem::new, 263);
        registerVanilla(ItemIds.DIAMOND, SimpleItem::new, 264);
        registerVanilla(ItemIds.IRON_INGOT, SimpleItem::new, 265);
        registerVanilla(ItemIds.GOLD_INGOT, SimpleItem::new, 266);
        registerVanilla(ItemIds.IRON_SWORD, ItemSwordIron::new, 267);
        registerVanilla(ItemIds.WOODEN_SWORD, ItemSwordWood::new, 268);
        registerVanilla(ItemIds.WOODEN_SHOVEL, ItemShovelWood::new, 269);
        registerVanilla(ItemIds.WOODEN_PICKAXE, ItemPickaxeWood::new, 270);
        registerVanilla(ItemIds.WOODEN_AXE, ItemAxeWood::new, 271);
        registerVanilla(ItemIds.STONE_SWORD, ItemSwordStone::new, 272);
        registerVanilla(ItemIds.STONE_SHOVEL, ItemShovelStone::new, 273);
        registerVanilla(ItemIds.STONE_PICKAXE, ItemPickaxeStone::new, 274);
        registerVanilla(ItemIds.STONE_AXE, ItemAxeStone::new, 275);
        registerVanilla(ItemIds.DIAMOND_SWORD, ItemSwordDiamond::new, 276);
        registerVanilla(ItemIds.DIAMOND_SHOVEL, ItemShovelDiamond::new, 277);
        registerVanilla(ItemIds.DIAMOND_PICKAXE, ItemPickaxeDiamond::new, 278);
        registerVanilla(ItemIds.DIAMOND_AXE, ItemAxeDiamond::new, 279);
        registerVanilla(ItemIds.STICK, SimpleItem::new, 280);
        registerVanilla(ItemIds.BOWL, SimpleItem::new, 281);
        registerVanilla(ItemIds.MUSHROOM_STEW, ItemMushroomStew::new, 282);
        registerVanilla(ItemIds.GOLDEN_SWORD, ItemSwordGold::new, 283);
        registerVanilla(ItemIds.GOLDEN_SHOVEL, ItemShovelGold::new, 284);
        registerVanilla(ItemIds.GOLDEN_PICKAXE, ItemPickaxeGold::new, 285);
        registerVanilla(ItemIds.GOLDEN_AXE, ItemAxeGold::new, 286);
        registerVanilla(ItemIds.STRING, PlaceableItem.factory(BlockIds.TRIPWIRE), 287);
        registerVanilla(ItemIds.FEATHER, SimpleItem::new, 288);
        registerVanilla(ItemIds.GUNPOWDER, SimpleItem::new, 289);
        registerVanilla(ItemIds.WOODEN_HOE, ItemHoeWood::new, 290);
        registerVanilla(ItemIds.STONE_HOE, ItemHoeStone::new, 291);
        registerVanilla(ItemIds.IRON_HOE, ItemHoeIron::new, 292);
        registerVanilla(ItemIds.DIAMOND_HOE, ItemHoeDiamond::new, 293);
        registerVanilla(ItemIds.GOLDEN_HOE, ItemHoeGold::new, 294);
        registerVanilla(ItemIds.WHEAT_SEEDS, PlaceableItem.factory(BlockIds.WHEAT), 295);
        registerVanilla(ItemIds.WHEAT, SimpleItem::new, 296);
        registerVanilla(ItemIds.BREAD, ItemBread::new, 297);
        registerVanilla(ItemIds.LEATHER_HELMET, ItemHelmetLeather::new, 298);
        registerVanilla(ItemIds.LEATHER_CHESTPLATE, ItemChestplateLeather::new, 299);
        registerVanilla(ItemIds.LEATHER_LEGGINGS, ItemLeggingsLeather::new, 300);
        registerVanilla(ItemIds.LEATHER_BOOTS, ItemBootsLeather::new, 301);
        registerVanilla(ItemIds.CHAINMAIL_HELMET, ItemHelmetChain::new, 302);
        registerVanilla(ItemIds.CHAINMAIL_CHESTPLATE, ItemChestplateChain::new, 303);
        registerVanilla(ItemIds.CHAINMAIL_LEGGINGS, ItemLeggingsChain::new, 304);
        registerVanilla(ItemIds.CHAINMAIL_BOOTS, ItemBootsChain::new, 305);
        registerVanilla(ItemIds.IRON_HELMET, ItemHelmetIron::new, 306);
        registerVanilla(ItemIds.IRON_CHESTPLATE, ItemChestplateIron::new, 307);
        registerVanilla(ItemIds.IRON_LEGGINGS, ItemLeggingsIron::new, 308);
        registerVanilla(ItemIds.IRON_BOOTS, ItemBootsIron::new, 309);
        registerVanilla(ItemIds.DIAMOND_HELMET, ItemHelmetDiamond::new, 310);
        registerVanilla(ItemIds.DIAMOND_CHESTPLATE, ItemChestplateDiamond::new, 311);
        registerVanilla(ItemIds.DIAMOND_LEGGINGS, ItemLeggingsDiamond::new, 312);
        registerVanilla(ItemIds.DIAMOND_BOOTS, ItemBootsDiamond::new, 313);
        registerVanilla(ItemIds.GOLDEN_HELMET, ItemHelmetGold::new, 314);
        registerVanilla(ItemIds.GOLDEN_CHESTPLATE, ItemChestplateGold::new, 315);
        registerVanilla(ItemIds.GOLDEN_LEGGINGS, ItemLeggingsGold::new, 316);
        registerVanilla(ItemIds.GOLDEN_BOOTS, ItemBootsGold::new, 317);
        registerVanilla(ItemIds.FLINT, SimpleItem::new, 318);
        registerVanilla(ItemIds.PORKCHOP, ItemPorkchopRaw::new, 319);
        registerVanilla(ItemIds.COOKED_PORKCHOP, ItemPorkchopCooked::new, 320);
        registerVanilla(ItemIds.PAINTING, ItemPainting::new, 321);
        registerVanilla(ItemIds.GOLDEN_APPLE, ItemAppleGold::new, 322);
        registerVanilla(ItemIds.SIGN, SignItem.factory(BlockIds.STANDING_SIGN), 323);
        registerVanilla(ItemIds.WOODEN_DOOR, PlaceableItem.factory(BlockIds.WOODEN_DOOR), 324);
        registerVanilla(ItemIds.BUCKET, ItemBucket::new, 325);

        registerVanilla(ItemIds.MINECART, ItemMinecart::new, 328);
        registerVanilla(ItemIds.SADDLE, ItemSaddle::new, 329);
        registerVanilla(ItemIds.IRON_DOOR, PlaceableItem.factory(BlockIds.IRON_DOOR), 330);
        registerVanilla(ItemIds.REDSTONE, PlaceableItem.factory(BlockIds.REDSTONE_WIRE), 331);
        registerVanilla(ItemIds.SNOWBALL, ItemSnowball::new, 332);
        registerVanilla(ItemIds.BOAT, ItemBoat::new, 333);
        registerVanilla(ItemIds.LEATHER, SimpleItem::new, 334);
        registerVanilla(ItemIds.KELP, PlaceableItem.factory(BlockIds.KELP), 335);
        registerVanilla(ItemIds.BRICK, SimpleItem::new, 336);
        registerVanilla(ItemIds.CLAY_BALL, SimpleItem::new, 337);
        registerVanilla(ItemIds.REEDS, PlaceableItem.factory(BlockIds.REEDS), 338);
        registerVanilla(ItemIds.PAPER, SimpleItem::new, 339);
        registerVanilla(ItemIds.BOOK, ItemBook::new, 340);
        registerVanilla(ItemIds.SLIME_BALL, SimpleItem::new, 341);
        registerVanilla(ItemIds.CHEST_MINECART, ItemMinecartChest::new, 342);

        registerVanilla(ItemIds.EGG, ItemEgg::new, 344);
        registerVanilla(ItemIds.COMPASS, SimpleItem::new, 345);
        registerVanilla(ItemIds.FISHING_ROD, ItemFishingRod::new, 346);
        registerVanilla(ItemIds.CLOCK, SimpleItem::new, 347);
        registerVanilla(ItemIds.GLOWSTONE_DUST, SimpleItem::new, 348);
        registerVanilla(ItemIds.FISH, ItemFish::new, 349);
        registerVanilla(ItemIds.COOKED_FISH, ItemFishCooked::new, 350);
        registerVanilla(ItemIds.DYE, ItemDye::new, 351);
        registerVanilla(ItemIds.BONE, SimpleItem::new, 352);
        registerVanilla(ItemIds.SUGAR, SimpleItem::new, 353);
        registerVanilla(ItemIds.CAKE, ItemCake::new, 354);
        registerVanilla(ItemIds.BED, ItemBed::new, 355);
        registerVanilla(ItemIds.REPEATER, PlaceableItem.factory(BlockIds.UNPOWERED_REPEATER), 356);
        registerVanilla(ItemIds.COOKIE, ItemCookie::new, 357);
        registerVanilla(ItemIds.MAP, ItemMap::new, 358);
        registerVanilla(ItemIds.SHEARS, ItemShears::new, 359);
        registerVanilla(ItemIds.MELON, ItemMelon::new, 360);
        registerVanilla(ItemIds.PUMPKIN_SEEDS, PlaceableItem.factory(BlockIds.PUMPKIN_STEM), 361);
        registerVanilla(ItemIds.MELON_SEEDS, PlaceableItem.factory(BlockIds.MELON_STEM), 362);
        registerVanilla(ItemIds.BEEF, ItemBeefRaw::new, 363);
        registerVanilla(ItemIds.COOKED_BEEF, ItemSteak::new, 364);
        registerVanilla(ItemIds.CHICKEN, ItemChickenRaw::new, 365);
        registerVanilla(ItemIds.COOKED_CHICKEN, ItemChickenCooked::new, 366);
        registerVanilla(ItemIds.ROTTEN_FLESH, ItemRottenFlesh::new, 367);
        registerVanilla(ItemIds.ENDER_PEARL, ItemEnderPearl::new, 368);
        registerVanilla(ItemIds.BLAZE_ROD, ItemBlazeRod::new, 369);
        registerVanilla(ItemIds.GHAST_TEAR, SimpleItem::new, 370);
        registerVanilla(ItemIds.GOLD_NUGGET, SimpleItem::new, 371);
        registerVanilla(ItemIds.NETHER_WART, PlaceableItem.factory(BlockIds.NETHER_WART), 372);
        registerVanilla(ItemIds.POTION, ItemPotion::new, 373);
        registerVanilla(ItemIds.GLASS_BOTTLE, ItemGlassBottle::new, 374);
        registerVanilla(ItemIds.SPIDER_EYE, SimpleItem::new, 375);
        registerVanilla(ItemIds.FERMENTED_SPIDER_EYE, SimpleItem::new, 376);
        registerVanilla(ItemIds.BLAZE_POWDER, SimpleItem::new, 377);
        registerVanilla(ItemIds.MAGMA_CREAM, SimpleItem::new, 378);
        registerVanilla(ItemIds.BREWING_STAND, PlaceableItem.factory(BlockIds.BREWING_STAND), 379);
        registerVanilla(ItemIds.CAULDRON, PlaceableItem.factory(BlockIds.CAULDRON), 380);
        registerVanilla(ItemIds.ENDER_EYE, SimpleItem::new, 381);
        registerVanilla(ItemIds.SPECKLED_MELON, SimpleItem::new, 382);
        registerVanilla(ItemIds.SPAWN_EGG, ItemSpawnEgg::new, 383);
        registerVanilla(ItemIds.EXPERIENCE_BOTTLE, ItemExpBottle::new, 384);
        registerVanilla(ItemIds.FIREBALL, ItemFireCharge::new, 385);
        //TODO: registerVanilla(WRITABLE_BOOK, ItemBookAndQuill::new, 386);
        registerVanilla(ItemIds.WRITTEN_BOOK, ItemBookWritten::new, 387);
        registerVanilla(ItemIds.EMERALD, SimpleItem::new, 388);
        registerVanilla(ItemIds.FRAME, PlaceableItem.factory(BlockIds.FRAME), 389);
        registerVanilla(ItemIds.FLOWER_POT, PlaceableItem.factory(BlockIds.FLOWER_POT), 390);
        registerVanilla(ItemIds.CARROT, ItemCarrot::new, 391);
        registerVanilla(ItemIds.POTATO, ItemPotato::new, 392);
        registerVanilla(ItemIds.BAKED_POTATO, ItemPotatoBaked::new, 393);
        registerVanilla(ItemIds.POISONOUS_POTATO, ItemPotatoPoisonous::new, 394);
        //TODO: registerVanilla(EMPTY_MAP, ItemEmptyMap::new, 395);
        registerVanilla(ItemIds.GOLDEN_CARROT, ItemCarrotGolden::new, 396);
        registerVanilla(ItemIds.SKULL, ItemSkull::new, 397);
        registerVanilla(ItemIds.CARROT_ON_A_STICK, ItemCarrotOnAStick::new, 398);
        registerVanilla(ItemIds.NETHER_STAR, SimpleItem::new, 399);
        registerVanilla(ItemIds.PUMPKIN_PIE, ItemPumpkinPie::new, 400);
        registerVanilla(ItemIds.FIREWORKS, ItemFirework::new, 401);

        registerVanilla(ItemIds.ENCHANTED_BOOK, ItemBookEnchanted::new, 403);
        registerVanilla(ItemIds.COMPARATOR, ItemRedstoneComparator::new, 404);
        registerVanilla(ItemIds.NETHERBRICK, SimpleItem::new, 405);
        registerVanilla(ItemIds.QUARTZ, ItemQuartz::new, 406);
        registerVanilla(ItemIds.TNT_MINECART, ItemMinecartTNT::new, 407);
        registerVanilla(ItemIds.HOPPER_MINECART, ItemMinecartHopper::new, 408);
        registerVanilla(ItemIds.PRISMARINE_SHARD, SimpleItem::new, 409);
        registerVanilla(ItemIds.HOPPER, PlaceableItem.factory(BlockIds.HOPPER), 410);
        registerVanilla(ItemIds.RABBIT, ItemRabbitRaw::new, 411);
        registerVanilla(ItemIds.COOKED_RABBIT, ItemRabbitCooked::new, 412);
        registerVanilla(ItemIds.RABBIT_STEW, ItemRabbitStew::new, 413);
        registerVanilla(ItemIds.RABBIT_FOOT, SimpleItem::new, 414);
        registerVanilla(ItemIds.RABBIT_HIDE, SimpleItem::new, 415);
        registerVanilla(ItemIds.HORSE_ARMOR_LEATHER, ItemHorseArmorLeather::new, 416);
        registerVanilla(ItemIds.HORSE_ARMOR_IRON, ItemHorseArmorIron::new, 417);
        registerVanilla(ItemIds.HORSE_ARMOR_GOLD, ItemHorseArmorGold::new, 418);
        registerVanilla(ItemIds.HORSE_ARMOR_DIAMOND, ItemHorseArmorDiamond::new, 419);
        //TODO: registerVanilla(LEAD, ItemLead::new, 420);
        //TODO: registerVanilla(NAME_TAG, ItemNameTag::new, 421);
        registerVanilla(ItemIds.PRISMARINE_CRYSTALS, SimpleItem::new, 422);
        registerVanilla(ItemIds.MUTTON_RAW, ItemMuttonRaw::new, 423);
        registerVanilla(ItemIds.MUTTON_COOKED, ItemMuttonCooked::new, 424);

        registerVanilla(ItemIds.ARMOR_STAND, SimpleItem::new, 425);
        registerVanilla(ItemIds.END_CRYSTAL, ItemEndCrystal::new, 426);
        registerVanilla(ItemIds.SPRUCE_DOOR, PlaceableItem.factory(BlockIds.SPRUCE_DOOR), 427);
        registerVanilla(ItemIds.BIRCH_DOOR, PlaceableItem.factory(BlockIds.BIRCH_DOOR), 428);
        registerVanilla(ItemIds.JUNGLE_DOOR, PlaceableItem.factory(BlockIds.JUNGLE_DOOR), 429);
        registerVanilla(ItemIds.ACACIA_DOOR, PlaceableItem.factory(BlockIds.ACACIA_DOOR), 430);
        registerVanilla(ItemIds.DARK_OAK_DOOR, PlaceableItem.factory(BlockIds.DARK_OAK_DOOR), 431);
        registerVanilla(ItemIds.CHORUS_FRUIT, ItemChorusFruit::new, 432);
        //TODO: registerVanilla(POPPED_CHORUS_FRUIT, ItemChorusFruitPopped::new, 433);

        //TODO: registerVanilla(DRAGON_BREATH, ItemDragonBreath::new, 437);
        registerVanilla(ItemIds.SPLASH_POTION, ItemPotionSplash::new, 438);

        registerVanilla(ItemIds.LINGERING_POTION, ItemPotionLingering::new, 441);

        //TODO: registerVanilla(ItemIds.COMMAND_BLOCK_MINECART, ItemElytra::new, 444);
        registerVanilla(ItemIds.ELYTRA, ItemElytra::new, 444);

        //TODO: registerVanilla(SHULKER_SHELL, ItemShulkerShell::new, 445);
        registerVanilla(ItemIds.BANNER, ItemBanner::new, 446);

        registerVanilla(ItemIds.IRON_NUGGET, SimpleItem::new, 452);
        registerVanilla(ItemIds.TRIDENT, ItemTrident::new, 455);

        registerVanilla(ItemIds.BEETROOT, ItemBeetroot::new, 457);
        registerVanilla(ItemIds.BEETROOT_SEEDS, PlaceableItem.factory(BlockIds.BEETROOT), 458);
        registerVanilla(ItemIds.BEETROOT_SOUP, ItemBeetrootSoup::new, 459);
        registerVanilla(ItemIds.SALMON, ItemSalmon::new, 460);
        registerVanilla(ItemIds.CLOWNFISH, ItemClownfish::new, 461);
        registerVanilla(ItemIds.PUFFERFISH, ItemPufferfish::new, 462);
        registerVanilla(ItemIds.COOKED_SALMON, ItemSalmonCooked::new, 463);
        registerVanilla(ItemIds.DRIED_KELP, ItemDriedKelp::new, 464);

        registerVanilla(ItemIds.APPLE_ENCHANTED, ItemAppleGoldEnchanted::new, 466);

        registerVanilla(ItemIds.TURTLE_HELMET, ItemTurtleShell::new, 469);
        registerVanilla(ItemIds.SPRUCE_SIGN, SignItem.factory(BlockIds.SPRUCE_STANDING_SIGN), 472);
        registerVanilla(ItemIds.BIRCH_SIGN, SignItem.factory(BlockIds.BIRCH_STANDING_SIGN), 473);
        registerVanilla(ItemIds.JUNGLE_SIGN, SignItem.factory(BlockIds.JUNGLE_STANDING_SIGN), 474);
        registerVanilla(ItemIds.ACACIA_SIGN, SignItem.factory(BlockIds.ACACIA_STANDING_SIGN), 475);
        registerVanilla(ItemIds.DARK_OAK_SIGN, SignItem.factory(BlockIds.DARK_OAK_STANDING_SIGN), 476);
        registerVanilla(ItemIds.SWEET_BERRIES, ItemSweetBerries::new, 477);

        registerVanilla(ItemIds.RECORD_CAT, RecordItem.factory("record.cat"), 500);
        registerVanilla(ItemIds.RECORD_13, RecordItem.factory("record.13"), 501);
        registerVanilla(ItemIds.RECORD_BLOCKS, RecordItem.factory("record.blocks"), 502);
        registerVanilla(ItemIds.RECORD_CHIRP, RecordItem.factory("record.chirp"), 503);
        registerVanilla(ItemIds.RECORD_FAR, RecordItem.factory("record.far"), 504);
        registerVanilla(ItemIds.RECORD_MALL, RecordItem.factory("record.mall"), 505);
        registerVanilla(ItemIds.RECORD_MELLOHI, RecordItem.factory("record.mellohi"), 506);
        registerVanilla(ItemIds.RECORD_STAL, RecordItem.factory("record.stal"), 507);
        registerVanilla(ItemIds.RECORD_STRAD, RecordItem.factory("record.strad"), 508);
        registerVanilla(ItemIds.RECORD_WARD, RecordItem.factory("record.ward"), 509);
        registerVanilla(ItemIds.RECORD_11, RecordItem.factory("record.11"), 510);
        registerVanilla(ItemIds.RECORD_WAIT, RecordItem.factory("record.wait"), 511);

        registerVanilla(ItemIds.SHIELD, ItemShield::new, 513);

        registerVanilla(ItemIds.CAMPFIRE, PlaceableItem.factory(BlockIds.CAMPFIRE), 720);

        registerVanilla(ItemIds.HONEYCOMB, SimpleItem::new, 736);
        registerVanilla(ItemIds.HONEY_BOTTLE, ItemHoneyBottle::new, 737);

        registerVanilla(ItemIds.NETHERITE_SWORD, ItemSwordNetherite::new, 743);
        registerVanilla(ItemIds.NETHERITE_SHOVEL, ItemShovelNetherite::new, 744);
        registerVanilla(ItemIds.NETHERITE_PICKAXE, ItemPickaxeNetherite::new, 745);
        registerVanilla(ItemIds.NETHERITE_AXE, ItemAxeNetherite::new, 746);
        registerVanilla(ItemIds.NETHERITE_HOE, ItemHoeNetherite::new, 747);
        registerVanilla(ItemIds.NETHERITE_HELMET, ItemHelmetNetherite::new, 748);
        registerVanilla(ItemIds.NETHERITE_CHESTPLATE, ItemChestplateNetherite::new, 749);
        registerVanilla(ItemIds.NETHERITE_LEGGINGS, ItemLeggingsNetherite::new, 750);
        registerVanilla(ItemIds.NETHERITE_BOOTS, ItemBootsNetherite::new, 751);

        registerVanilla(ItemIds.CRIMSON_SIGN, PlaceableItem.factory(BlockIds.CRIMSON_STANDING_SIGN), 753);
        registerVanilla(ItemIds.WARPED_SIGN, PlaceableItem.factory(BlockIds.WARPED_STANDING_SIGN), 754);
        registerVanilla(ItemIds.CRIMSON_DOOR, PlaceableItem.factory(BlockIds.CRIMSON_DOOR), 755);
        registerVanilla(ItemIds.WARPED_DOOR, PlaceableItem.factory(BlockIds.WARPED_DOOR), 756);
    }

    @Getter
    private static class ItemData {
        private String name;
        private int id;
    }
}
