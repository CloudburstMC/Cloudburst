package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

@UtilityClass
public class ItemTypes {

    public static final ItemType IRON_SHOVEL = ItemType.of(ItemIds.IRON_SHOVEL); //.maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(IRON).attackDamage(4).build();
    public static final ItemType IRON_PICKAXE = ItemType.of(ItemIds.IRON_PICKAXE); //.maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(IRON).attackDamage(4).build();
    public static final ItemType IRON_AXE = ItemType.of(ItemIds.IRON_AXE); //.maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(IRON).attackDamage(6).build();
    public static final ItemType FLINT_AND_STEEL = ItemType.of(ItemIds.FLINT_AND_STEEL); //.maxStackSize(1).build();
    public static final ItemType APPLE = ItemType.of(ItemIds.APPLE); //.maxStackSize(64).build();
    public static final ItemType BOW = ItemType.of(ItemIds.BOW); //.maxStackSize(1).fuelTime((short) 200).build();
    public static final ItemType ARROW = ItemType.of(ItemIds.ARROW); //.maxStackSize(64).build();
    public static final ItemType COAL = ItemType.of(ItemIds.COAL); //.maxStackSize(64).data(Coal.class).fuelTime((short) 1600).build();
    public static final ItemType CHARCOAL = ItemType.of(ItemIds.CHARCOAL);
    public static final ItemType DIAMOND = ItemType.of(ItemIds.DIAMOND); //.maxStackSize(64).build();
    public static final ItemType IRON_INGOT = ItemType.of(ItemIds.IRON_INGOT); //.maxStackSize(64).build();
    public static final ItemType GOLD_INGOT = ItemType.of(ItemIds.GOLD_INGOT); //.maxStackSize(64).build();
    public static final ItemType IRON_SWORD = ItemType.of(ItemIds.IRON_SWORD); //.maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(IRON).attackDamage(7).build();
    public static final ItemType WOODEN_SWORD = ItemType.of(ItemIds.WOODEN_SWORD); //.maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(WOOD).attackDamage(5).fuelTime((short) 200).build();
    public static final ItemType WOODEN_SHOVEL = ItemType.of(ItemIds.WOODEN_SHOVEL); //.maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(WOOD).attackDamage(2).fuelTime((short) 200).build();
    public static final ItemType WOODEN_PICKAXE = ItemType.of(ItemIds.WOODEN_PICKAXE); //.maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(WOOD).attackDamage(2).fuelTime((short) 200).build();
    public static final ItemType WOODEN_AXE = ItemType.of(ItemIds.WOODEN_AXE); //.maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(WOOD).attackDamage(4).fuelTime((short) 200).build();
    public static final ItemType STONE_SWORD = ItemType.of(ItemIds.STONE_SWORD); //.maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(STONE).attackDamage(6).build();
    public static final ItemType STONE_SHOVEL = ItemType.of(ItemIds.STONE_SHOVEL); //.maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(STONE).attackDamage(3).build();
    public static final ItemType STONE_PICKAXE = ItemType.of(ItemIds.STONE_PICKAXE); //.maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(STONE).attackDamage(3).build();
    public static final ItemType STONE_AXE = ItemType.of(ItemIds.STONE_AXE); //.maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(STONE).attackDamage(5).build();
    public static final ItemType DIAMOND_SWORD = ItemType.of(ItemIds.DIAMOND_SWORD); //.maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(TierTypes.DIAMOND).attackDamage(8).build();
    public static final ItemType DIAMOND_SHOVEL = ItemType.of(ItemIds.DIAMOND_SHOVEL); //.maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(TierTypes.DIAMOND).attackDamage(5).build();
    public static final ItemType DIAMOND_PICKAXE = ItemType.of(ItemIds.DIAMOND_PICKAXE); //.maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(TierTypes.DIAMOND).attackDamage(5).build();
    public static final ItemType DIAMOND_AXE = ItemType.of(ItemIds.DIAMOND_AXE); //.maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(TierTypes.DIAMOND).attackDamage(7).build();
    public static final ItemType STICK = ItemType.of(ItemIds.STICK); //.maxStackSize(64).fuelTime((short) 100).build();
    public static final ItemType BOWL = ItemType.of(ItemIds.BOWL); //.maxStackSize(64).fuelTime((short) 200).build();
    public static final ItemType MUSHROOM_STEW = ItemType.of(ItemIds.MUSHROOM_STEW); //.maxStackSize(1).build();
    public static final ItemType GOLDEN_SWORD = ItemType.of(ItemIds.GOLDEN_SWORD); //.maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(GOLD).attackDamage(5).build();
    public static final ItemType GOLDEN_SHOVEL = ItemType.of(ItemIds.GOLDEN_SHOVEL); //.maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(GOLD).attackDamage(2).build();
    public static final ItemType GOLDEN_PICKAXE = ItemType.of(ItemIds.GOLDEN_PICKAXE); //.maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(GOLD).attackDamage(2).build();
    public static final ItemType GOLDEN_AXE = ItemType.of(ItemIds.GOLDEN_AXE); //.maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(GOLD).attackDamage(4).build();
    public static final ItemType STRING = ItemType.of(ItemIds.STRING); //.maxStackSize(64).blockType(BlockTypes.TRIPWIRE).build();
    public static final ItemType FEATHER = ItemType.of(ItemIds.FEATHER); //.maxStackSize(64).build();
    public static final ItemType GUNPOWDER = ItemType.of(ItemIds.GUNPOWDER); //.maxStackSize(64).build();
    public static final ItemType WOODEN_HOE = ItemType.of(ItemIds.WOODEN_HOE); //.maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(WOOD).attackDamage(2).fuelTime((short) 200).build();
    public static final ItemType STONE_HOE = ItemType.of(ItemIds.STONE_HOE); //.maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(STONE).attackDamage(3).build();
    public static final ItemType IRON_HOE = ItemType.of(ItemIds.IRON_HOE); //.maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(IRON).attackDamage(4).build();
    public static final ItemType DIAMOND_HOE = ItemType.of(ItemIds.DIAMOND_HOE); //.maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(TierTypes.DIAMOND).attackDamage(5).build();
    public static final ItemType GOLDEN_HOE = ItemType.of(ItemIds.GOLDEN_HOE); //.maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(GOLD).attackDamage(2).build();
    public static final ItemType WHEAT_SEEDS = ItemType.of(ItemIds.WHEAT_SEEDS); //.maxStackSize(64).blockType(BlockTypes.WHEAT).build();
    public static final ItemType WHEAT = ItemType.of(ItemIds.WHEAT); //.maxStackSize(64).build();
    public static final ItemType BREAD = ItemType.of(ItemIds.BREAD); //.maxStackSize(64).build();
    public static final ItemType LEATHER_HELMET = ItemType.of(ItemIds.LEATHER_HELMET); //.maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType LEATHER_CHESTPLATE = ItemType.of(ItemIds.LEATHER_CHESTPLATE); //.maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType LEATHER_LEGGINGS = ItemType.of(ItemIds.LEATHER_LEGGINGS); //.maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType LEATHER_BOOTS = ItemType.of(ItemIds.LEATHER_BOOTS); //.maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType CHAINMAIL_HELMET = ItemType.of(ItemIds.CHAINMAIL_HELMET); //.maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType CHAINMAIL_CHESTPLATE = ItemType.of(ItemIds.CHAINMAIL_CHESTPLATE); //.maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType CHAINMAIL_LEGGINGS = ItemType.of(ItemIds.CHAINMAIL_LEGGINGS); //.maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType CHAINMAIL_BOOTS = ItemType.of(ItemIds.CHAINMAIL_BOOTS); //.maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType IRON_HELMET = ItemType.of(ItemIds.IRON_HELMET); //.maxStackSize(1).tierType(IRON).build();
    public static final ItemType IRON_CHESTPLATE = ItemType.of(ItemIds.IRON_CHESTPLATE); //.maxStackSize(1).tierType(IRON).build();
    public static final ItemType IRON_LEGGINGS = ItemType.of(ItemIds.IRON_LEGGINGS); //.maxStackSize(1).tierType(IRON).build();
    public static final ItemType IRON_BOOTS = ItemType.of(ItemIds.IRON_BOOTS); //.maxStackSize(1).tierType(IRON).build();
    public static final ItemType DIAMOND_HELMET = ItemType.of(ItemIds.DIAMOND_HELMET); //.maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType DIAMOND_CHESTPLATE = ItemType.of(ItemIds.DIAMOND_CHESTPLATE); //.maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType DIAMOND_LEGGINGS = ItemType.of(ItemIds.DIAMOND_LEGGINGS); //.maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType DIAMOND_BOOTS = ItemType.of(ItemIds.DIAMOND_BOOTS); //.maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType GOLDEN_HELMET = ItemType.of(ItemIds.GOLDEN_HELMET); //.maxStackSize(1).tierType(GOLD).build();
    public static final ItemType GOLDEN_CHESTPLATE = ItemType.of(ItemIds.GOLDEN_CHESTPLATE); //.maxStackSize(1).tierType(GOLD).build();
    public static final ItemType GOLDEN_LEGGINGS = ItemType.of(ItemIds.GOLDEN_LEGGINGS); //.maxStackSize(1).tierType(GOLD).build();
    public static final ItemType GOLDEN_BOOTS = ItemType.of(ItemIds.GOLDEN_BOOTS); //.maxStackSize(1).tierType(GOLD).build();
    public static final ItemType FLINT = ItemType.of(ItemIds.FLINT); //.maxStackSize(64).build();
    public static final ItemType PORKCHOP = ItemType.of(ItemIds.PORKCHOP); //.maxStackSize(64).build();
    public static final ItemType COOKED_PORKCHOP = ItemType.of(ItemIds.COOKED_PORKCHOP); //.maxStackSize(64).build();
    public static final ItemType PAINTING = ItemType.of(ItemIds.PAINTING); //.maxStackSize(64).build();
    public static final ItemType GOLDEN_APPLE = ItemType.of(ItemIds.GOLDEN_APPLE); //.maxStackSize(64).build();
    public static final ItemType SIGN = ItemType.of(ItemIds.SIGN); //.maxStackSize(16).blockType(BlockTypes.STANDING_SIGN).data(TreeSpecies.class).build();
    public static final ItemType WOODEN_DOOR = ItemType.of(ItemIds.WOODEN_DOOR); //.maxStackSize(64).blockType(BlockTypes.WOODEN_DOOR).data(TreeSpecies.class).build();
    public static final ItemType BUCKET = ItemType.of(ItemIds.BUCKET); //.data(Bucket.class).maxStackSize(16).build();
    public static final ItemType MILK_BUCKET = ItemType.of(ItemIds.MILK_BUCKET);
    public static final ItemType COD_BUCKET = ItemType.of(ItemIds.COD_BUCKET);
    public static final ItemType SALMON_BUCKET = ItemType.of(ItemIds.SALMON_BUCKET);
    public static final ItemType TROPICAL_FISH_BUCKET = ItemType.of(ItemIds.TROPICAL_FISH_BUCKET);
    public static final ItemType PUFFERFISH_BUCKET = ItemType.of(ItemIds.PUFFERFISH_BUCKET);
    public static final ItemType WATER_BUCKET = ItemType.of(ItemIds.WATER_BUCKET);
    public static final ItemType LAVA_BUCKET = ItemType.of(ItemIds.LAVA_BUCKET);
    public static final ItemType MINECART = ItemType.of(ItemIds.MINECART); //.maxStackSize(1).build();
    public static final ItemType SADDLE = ItemType.of(ItemIds.SADDLE); //.maxStackSize(1).build();
    public static final ItemType IRON_DOOR = ItemType.of(ItemIds.IRON_DOOR); //.maxStackSize(64).blockType(BlockTypes.IRON_DOOR).build();
    public static final ItemType REDSTONE = ItemType.of(ItemIds.REDSTONE); //.maxStackSize(64).blockType(BlockTypes.REDSTONE_WIRE).build();
    public static final ItemType SNOWBALL = ItemType.of(ItemIds.SNOWBALL); //.maxStackSize(16).build();
    public static final ItemType BOAT = ItemType.of(ItemIds.BOAT); //.maxStackSize(1).fuelTime((short) 1200).build();
    public static final ItemType LEATHER = ItemType.of(ItemIds.LEATHER); //.maxStackSize(64).build();
    public static final ItemType KELP = ItemType.of(ItemIds.KELP); //.maxStackSize(64).blockType(BlockTypes.KELP).build();
    public static final ItemType BRICK = ItemType.of(ItemIds.BRICK); //.maxStackSize(64).build();
    public static final ItemType CLAY_BALL = ItemType.of(ItemIds.CLAY_BALL); //.maxStackSize(64).build();
    public static final ItemType REEDS = ItemType.of(ItemIds.SUGAR_CANE); //.maxStackSize(64).blockType(BlockTypes.REEDS).build();
    public static final ItemType PAPER = ItemType.of(ItemIds.PAPER); //.maxStackSize(64).build();
    public static final ItemType BOOK = ItemType.of(ItemIds.BOOK); //.maxStackSize(64).build();
    public static final ItemType SLIME_BALL = ItemType.of(ItemIds.SLIME_BALL); //.maxStackSize(64).build();
    public static final ItemType CHEST_MINECART = ItemType.of(ItemIds.CHEST_MINECART); //.maxStackSize(1).build();
    public static final ItemType EGG = ItemType.of(ItemIds.EGG); //.maxStackSize(16).build();
    public static final ItemType COMPASS = ItemType.of(ItemIds.COMPASS); //.maxStackSize(64).build();
    public static final ItemType FISHING_ROD = ItemType.of(ItemIds.FISHING_ROD); //.maxStackSize(1).fuelTime((short) 300).build();
    public static final ItemType CLOCK = ItemType.of(ItemIds.CLOCK); //.maxStackSize(64).build();
    public static final ItemType GLOWSTONE_DUST = ItemType.of(ItemIds.GLOWSTONE_DUST); //.maxStackSize(64).build();
    public static final ItemType FISH = ItemType.of(ItemIds.COD); //.maxStackSize(64).build();
    public static final ItemType COOKED_FISH = ItemType.of(ItemIds.COOKED_COD); //.maxStackSize(64).build();
    public static final ItemType DYE = ItemType.of(ItemIds.INK_SAC); //.maxStackSize(64).data(DyeColor.class).build();
    public static final ItemType BONE = ItemType.of(ItemIds.BONE); //.maxStackSize(64).build();
    public static final ItemType SUGAR = ItemType.of(ItemIds.SUGAR); //.maxStackSize(64).build();
    public static final ItemType CAKE = ItemType.of(ItemIds.CAKE); //.maxStackSize(1).build();
    public static final ItemType BED = ItemType.of(ItemIds.BED); //.maxStackSize(1).data(DyeColor.class).build();
    public static final ItemType REPEATER = ItemType.of(ItemIds.REPEATER); //.maxStackSize(64).blockType(BlockTypes.REPEATER).build();
    public static final ItemType COOKIE = ItemType.of(ItemIds.COOKIE); //.maxStackSize(64).build();
    public static final ItemType MAP = ItemType.of(ItemIds.MAP); //.maxStackSize(64).build();
    public static final ItemType SHEARS = ItemType.of(ItemIds.SHEARS); //.maxStackSize(1).data(Damageable.class).toolType(ToolTypes.SHEARS).build();
    public static final ItemType MELON = ItemType.of(ItemIds.MELON); //.maxStackSize(64).build();
    public static final ItemType PUMPKIN_SEEDS = ItemType.of(ItemIds.PUMPKIN_SEEDS); //.maxStackSize(64).blockType(BlockTypes.PUMPKIN_STEM).build();
    public static final ItemType MELON_SEEDS = ItemType.of(ItemIds.MELON_SEEDS); //.maxStackSize(64).blockType(BlockTypes.MELON_STEM).build();
    public static final ItemType BEEF = ItemType.of(ItemIds.BEEF); //.maxStackSize(64).build();
    public static final ItemType COOKED_BEEF = ItemType.of(ItemIds.COOKED_BEEF); //.maxStackSize(64).build();
    public static final ItemType CHICKEN = ItemType.of(ItemIds.CHICKEN); //.maxStackSize(64).build();
    public static final ItemType COOKED_CHICKEN = ItemType.of(ItemIds.COOKED_CHICKEN); //.maxStackSize(64).build();
    public static final ItemType ROTTEN_FLESH = ItemType.of(ItemIds.ROTTEN_FLESH); //.maxStackSize(64).build();
    public static final ItemType ENDER_PEARL = ItemType.of(ItemIds.ENDER_PEARL); //.maxStackSize(64).build();
    public static final ItemType BLAZE_ROD = ItemType.of(ItemIds.BLAZE_ROD); //.maxStackSize(64).fuelTime((short) 2400).build();
    public static final ItemType GHAST_TEAR = ItemType.of(ItemIds.GHAST_TEAR); //.maxStackSize(64).build();
    public static final ItemType GOLD_NUGGET = ItemType.of(ItemIds.GOLD_NUGGET); //.maxStackSize(64).build();
    public static final ItemType NETHER_WART = ItemType.of(ItemIds.NETHER_WART); //.maxStackSize(64).blockType(BlockTypes.NETHER_WART).build();
    public static final ItemType POTION = ItemType.of(ItemIds.POTION); //.maxStackSize(1).build();
    public static final ItemType GLASS_BOTTLE = ItemType.of(ItemIds.GLASS_BOTTLE); //.maxStackSize(64).build();
    public static final ItemType SPIDER_EYE = ItemType.of(ItemIds.SPIDER_EYE); //.maxStackSize(64).build();
    public static final ItemType FERMENTED_SPIDER_EYE = ItemType.of(ItemIds.FERMENTED_SPIDER_EYE); //.maxStackSize(64).build();
    public static final ItemType BLAZE_POWDER = ItemType.of(ItemIds.BLAZE_POWDER); //.maxStackSize(64).build();
    public static final ItemType MAGMA_CREAM = ItemType.of(ItemIds.MAGMA_CREAM); //.maxStackSize(64).build();
    public static final ItemType BREWING_STAND = ItemType.of(ItemIds.BREWING_STAND); //.maxStackSize(64).blockType(BlockTypes.BREWING_STAND).build();
    public static final ItemType CAULDRON = ItemType.of(ItemIds.CAULDRON); //.maxStackSize(64).blockType(BlockTypes.CAULDRON).build();
    public static final ItemType ENDER_EYE = ItemType.of(ItemIds.ENDER_EYE); //.maxStackSize(64).build();
    public static final ItemType SPECKLED_MELON = ItemType.of(ItemIds.SPECKLED_MELON); //.maxStackSize(64).build();
    public static final ItemType SPAWN_EGG = ItemType.of(ItemIds.SPAWN_EGG); //.maxStackSize(64).build();
    public static final ItemType EXPERIENCE_BOTTLE = ItemType.of(ItemIds.EXPERIENCE_BOTTLE); //.maxStackSize(64).build();
    public static final ItemType FIREBALL = ItemType.of(ItemIds.FIRE_CHARGE); //.maxStackSize(64).build();
    public static final ItemType WRITABLE_BOOK = ItemType.of(ItemIds.WRITABLE_BOOK); //.maxStackSize(1).build();
    public static final ItemType WRITTEN_BOOK = ItemType.of(ItemIds.WRITTEN_BOOK); //.maxStackSize(16).build();
    public static final ItemType EMERALD = ItemType.of(ItemIds.EMERALD); //.maxStackSize(64).build();
    public static final ItemType FRAME = ItemType.of(ItemIds.FRAME); //.maxStackSize(64).blockType(BlockTypes.FRAME).build();
    public static final ItemType FLOWER_POT = ItemType.of(ItemIds.FLOWER_POT); //.maxStackSize(64).blockType(BlockTypes.FLOWER_POT).build();
    public static final ItemType CARROT = ItemType.of(ItemIds.CARROT); //.maxStackSize(64).build();
    public static final ItemType POTATO = ItemType.of(ItemIds.POTATO); //.maxStackSize(64).build();
    public static final ItemType BAKED_POTATO = ItemType.of(ItemIds.BAKED_POTATO); //.maxStackSize(64).build();
    public static final ItemType POISONOUS_POTATO = ItemType.of(ItemIds.POISONOUS_POTATO); //.maxStackSize(64).build();
    public static final ItemType EMPTY_MAP = ItemType.of(ItemIds.EMPTY_MAP); //.maxStackSize(64).build();
    public static final ItemType GOLDEN_CARROT = ItemType.of(ItemIds.GOLDEN_CARROT); //.maxStackSize(64).build();
    public static final ItemType SKULL = ItemType.of(ItemIds.SKULL); //.maxStackSize(64).build(); //TODO: skull type
    public static final ItemType CARROT_ON_A_STICK = ItemType.of(ItemIds.CARROT_ON_A_STICK); //.maxStackSize(1).build();
    public static final ItemType NETHER_STAR = ItemType.of(ItemIds.NETHER_STAR); //.maxStackSize(64).build();
    public static final ItemType PUMPKIN_PIE = ItemType.of(ItemIds.PUMPKIN_PIE); //.maxStackSize(64).build();
    public static final ItemType FIREWORKS = ItemType.of(ItemIds.FIREWORK_ROCKET); //.maxStackSize(64).build();
    public static final ItemType FIREWORKS_CHARGE = ItemType.of(ItemIds.FIREWORKS_CHARGE); //.maxStackSize(64).build();
    public static final ItemType ENCHANTED_BOOK = ItemType.of(ItemIds.ENCHANTED_BOOK); //.maxStackSize(1).build();
    public static final ItemType COMPARATOR = ItemType.of(ItemIds.COMPARATOR); //.maxStackSize(64).blockType(BlockTypes.COMPARATOR).build();
    public static final ItemType NETHERBRICK = ItemType.of(ItemIds.NETHERBRICK); //.maxStackSize(64).build();
    public static final ItemType QUARTZ = ItemType.of(ItemIds.QUARTZ); //.maxStackSize(64).build();
    public static final ItemType TNT_MINECART = ItemType.of(ItemIds.TNT_MINECART); //.maxStackSize(1).build();
    public static final ItemType HOPPER_MINECART = ItemType.of(ItemIds.HOPPER_MINECART); //.maxStackSize(1).build();
    public static final ItemType PRISMARINE_SHARD = ItemType.of(ItemIds.PRISMARINE_SHARD); //.maxStackSize(64).build();
    public static final ItemType HOPPER = ItemType.of(ItemIds.HOPPER); //.maxStackSize(64).blockType(BlockTypes.HOPPER).build();
    public static final ItemType RABBIT = ItemType.of(ItemIds.RABBIT); //.maxStackSize(64).build();
    public static final ItemType COOKED_RABBIT = ItemType.of(ItemIds.COOKED_RABBIT); //.maxStackSize(64).build();
    public static final ItemType RABBIT_STEW = ItemType.of(ItemIds.RABBIT_STEW); //.maxStackSize(64).build();
    public static final ItemType RABBIT_FOOT = ItemType.of(ItemIds.RABBIT_FOOT); //.maxStackSize(64).build();
    public static final ItemType RABBIT_HIDE = ItemType.of(ItemIds.RABBIT_HIDE); //.maxStackSize(64).build();
    public static final ItemType HORSE_ARMOR_LEATHER = ItemType.of(ItemIds.HORSE_ARMOR_LEATHER); //.maxStackSize(1).build();
    public static final ItemType HORSE_ARMOR_IRON = ItemType.of(ItemIds.HORSE_ARMOR_IRON); //.maxStackSize(1).build();
    public static final ItemType HORSE_ARMOR_GOLD = ItemType.of(ItemIds.HORSE_ARMOR_GOLD); //.maxStackSize(1).build();
    public static final ItemType HORSE_ARMOR_DIAMOND = ItemType.of(ItemIds.HORSE_ARMOR_DIAMOND); //.maxStackSize(1).build();
    public static final ItemType LEAD = ItemType.of(ItemIds.LEAD); //.maxStackSize(64).build();
    public static final ItemType NAME_TAG = ItemType.of(ItemIds.NAME_TAG); //.maxStackSize(64).build();
    public static final ItemType PRISMARINE_CRYSTALS = ItemType.of(ItemIds.PRISMARINE_CRYSTALS); //.maxStackSize(64).build();
    public static final ItemType MUTTON_RAW = ItemType.of(ItemIds.MUTTON_RAW); //.maxStackSize(64).build();
    public static final ItemType MUTTON_COOKED = ItemType.of(ItemIds.MUTTON_COOKED); //.maxStackSize(64).build();
    public static final ItemType ARMOR_STAND = ItemType.of(ItemIds.ARMOR_STAND); //.maxStackSize(64).build();
    public static final ItemType END_CRYSTAL = ItemType.of(ItemIds.END_CRYSTAL); //.maxStackSize(64).build();
    public static final ItemType CHORUS_FRUIT = ItemType.of(ItemIds.CHORUS_FRUIT); //.maxStackSize(64).build();
    public static final ItemType CHORUS_FRUIT_POPPED = ItemType.of(ItemIds.CHORUS_FRUIT_POPPED); //.maxStackSize(64).build();
    public static final ItemType DRAGON_BREATH = ItemType.of(ItemIds.DRAGON_BREATH); //.maxStackSize(64).build();
    public static final ItemType SPLASH_POTION = ItemType.of(ItemIds.SPLASH_POTION); //.maxStackSize(1).build();
    public static final ItemType LINGERING_POTION = ItemType.of(ItemIds.LINGERING_POTION); //.maxStackSize(1).build();
    public static final ItemType COMMAND_BLOCK_MINECART = ItemType.of(ItemIds.COMMAND_BLOCK_MINECART); //.maxStackSize(1).build();
    public static final ItemType ELYTRA = ItemType.of(ItemIds.ELYTRA); //.maxStackSize(1).build();
    public static final ItemType SHULKER_SHELL = ItemType.of(ItemIds.SHULKER_SHELL); //.maxStackSize(64).build();
    public static final ItemType BANNER = ItemType.of(ItemIds.BANNER); //.maxStackSize(16).fuelTime((short) 300).build();
    public static final ItemType BANNER_PATTERN = ItemType.of(ItemIds.BANNER_PATTERN); //.maxStackSize(1).build();
    public static final ItemType TOTEM = ItemType.of(ItemIds.TOTEM); //.maxStackSize(1).build();
    public static final ItemType IRON_NUGGET = ItemType.of(ItemIds.IRON_NUGGET); //.maxStackSize(64).build();
    public static final ItemType BOARD = ItemType.of(ItemIds.BOARD); //.maxStackSize(16).build();
    public static final ItemType PORTFOLIO = ItemType.of(ItemIds.PORTFOLIO); //.maxStackSize(64).build();
    public static final ItemType TRIDENT = ItemType.of(ItemIds.TRIDENT); //.maxStackSize(64).build();
    public static final ItemType BEETROOT = ItemType.of(ItemIds.BEETROOT); //.maxStackSize(54).blockType(BlockTypes.BEETROOT).build();
    public static final ItemType BEETROOT_SEEDS = ItemType.of(ItemIds.BEETROOT_SEEDS); //.maxStackSize(64).build();
    public static final ItemType BEETROOT_SOUP = ItemType.of(ItemIds.BEETROOT_SOUP); //.maxStackSize(1).build();
    public static final ItemType SALMON = ItemType.of(ItemIds.SALMON); //.maxStackSize(64).build();
    public static final ItemType CLOWNFISH = ItemType.of(ItemIds.TROPICAL_FISH); //.maxStackSize(64).build();
    public static final ItemType PUFFERFISH = ItemType.of(ItemIds.PUFFERFISH); //.maxStackSize(64).build();
    public static final ItemType COOKED_SALMON = ItemType.of(ItemIds.COOKED_SALMON); //.maxStackSize(64).build();
    public static final ItemType NAUTILUS_SHELL = ItemType.of(ItemIds.NAUTILUS_SHELL); //.maxStackSize(64).build();
    public static final ItemType DRIED_KELP = ItemType.of(ItemIds.DRIED_KELP); //.maxStackSize(64).build();
    public static final ItemType APPLE_ENCHANTED = ItemType.of(ItemIds.APPLE_ENCHANTED); //.maxStackSize(64).build();
    public static final ItemType HEART_OF_THE_SEA = ItemType.of(ItemIds.HEART_OF_THE_SEA); //.maxStackSize(64).build();
    public static final ItemType TURTLE_SHELL_PIECE = ItemType.of(ItemIds.TURTLE_SHELL_PIECE); //.maxStackSize(64).build();
    public static final ItemType TURTLE_HELMET = ItemType.of(ItemIds.TURTLE_HELMET); //.maxStackSize(1).build();
    public static final ItemType PHANTOM_MEMBRANE = ItemType.of(ItemIds.PHANTOM_MEMBRANE); //.maxStackSize(64).build();
    public static final ItemType CROSSBOW = ItemType.of(ItemIds.CROSSBOW); //.maxStackSize(1).build();
    public static final ItemType SWEET_BERRIES = ItemType.of(ItemIds.SWEET_BERRIES); //.maxStackSize(64).build();
    public static final ItemType CAMERA = ItemType.of(ItemIds.CAMERA); //.maxStackSize(64).build();
    public static final ItemType RECORD = ItemType.of(ItemIds.RECORD_13); //.maxStackSize(1).build();

    public static final ItemType SUSPICIOUS_STEW = ItemType.of(ItemIds.SUSPICIOUS_STEW); //.build();
    public static final ItemType LODESTONE_COMPASS = ItemType.of(ItemIds.LODESTONE_COMPASS); //.build();
    public static final ItemType WARPED_FUNGUS_ON_A_STICK = ItemType.of(ItemIds.WARPED_FUNGUS_ON_STICK); //.build();


    public static final ItemType SHIELD = ItemType.of(ItemIds.SHIELD); //.maxStackSize(1).data(Damageable.class).build();
    public static final ItemType CAMPFIRE = ItemType.of(ItemIds.CAMPFIRE); //.maxStackSize(1).blockType(BlockTypes.CAMPFIRE).build();
    public static final ItemType HONEYCOMB = ItemType.of(ItemIds.HONEYCOMB); //.maxStackSize(64).build();
    public static final ItemType HONEY_BOTTLE = ItemType.of(ItemIds.HONEY_BOTTLE); //.maxStackSize(16).build();
    public static final ItemType NETHERITE_INGOT = ItemType.of(ItemIds.NETHERITE_INGOT); //.maxStackSize(64).build();
    public static final ItemType NETHERITE_SCRAP = ItemType.of(ItemIds.NETHERITE_SCRAP); //.build();
    public static final ItemType NETHERITE_SWORD = ItemType.of(ItemIds.NETHERITE_SWORD); //.maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(NETHERITE).attackDamage(9).build();
    public static final ItemType NETHERITE_SHOVEL = ItemType.of(ItemIds.NETHERITE_SHOVEL); //.maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(NETHERITE).attackDamage(6).build();
    public static final ItemType NETHERITE_PICKAXE = ItemType.of(ItemIds.NETHERITE_PICKAXE); //.maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(NETHERITE).attackDamage(6).build();
    public static final ItemType NETHERITE_AXE = ItemType.of(ItemIds.NETHERITE_AXE); //.maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(NETHERITE).attackDamage(8).build();
    public static final ItemType NETHERITE_HOE = ItemType.of(ItemIds.NETHERITE_HOE); //.maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(NETHERITE).attackDamage(6).build();
    public static final ItemType NETHERITE_HELMET = ItemType.of(ItemIds.NETHERITE_HELMET); //.maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType NETHERITE_CHESTPLATE = ItemType.of(ItemIds.NETHERITE_CHESTPLATE); //.maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType NETHERITE_LEGGINGS = ItemType.of(ItemIds.NETHERITE_LEGGINGS); //.maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType NETHERITE_BOOTS = ItemType.of(ItemIds.NETHERITE_BOOTS); //.maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType CONCRETE_POWDER = ItemType.of(ItemIds.CONCRETE_POWDER); //.maxStackSize(64).data(DyeColor.class).blockType(BlockTypes.CONCRETE_POWDER).build();
    public static final ItemType UNKNOWN = ItemType.of(Identifiers.UNKNOWN);

    public static final ItemType AMETHYST_SHARD = ItemType.of(ItemIds.AMETHYST_SHARD); //.build();
    public static final ItemType GLOW_BERRIES = ItemType.of(ItemIds.GLOW_BERRIES); //.build();
    public static final ItemType GLOW_INK_SAC = ItemType.of(ItemIds.GLOW_INK_SAC); //.build();
    public static final ItemType GOAT_HORN = ItemType.of(ItemIds.GOAT_HORN); //.build();
    public static final ItemType GLOW_FRAME = ItemType.of(ItemIds.GLOW_FRAME); //.build();
    public static final ItemType RAW_COPPER = ItemType.of(ItemIds.RAW_COPPER); //.build();
    public static final ItemType RAW_GOLD = ItemType.of(ItemIds.RAW_GOLD); //.build();
    public static final ItemType RAW_IRON = ItemType.of(ItemIds.RAW_IRON); //.build();
    public static final ItemType SPYGLASS = ItemType.of(ItemIds.SPYGLASS); //.build();
    public static final ItemType COPPER_INGOT = ItemType.of(ItemIds.COPPER_INGOT); //.build();

    public static final ItemType LAPIS_LAZULI = ItemType.of(ItemIds.LAPIS_LAZULI);

    public static final ItemType DISC_FRAGMENT_5 = ItemType.of(ItemIds.DISC_FRAGMENT_5);
    public static final ItemType RECOVERY_COMPASS = ItemType.of(ItemIds.RECOVERY_COMPASS);
    public static final ItemType ECHO_CHARD = ItemType.of(ItemIds.ECHO_CHARD);
}
