package org.cloudburstmc.server.item;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.item.data.Coal;
import org.cloudburstmc.server.item.data.Damageable;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import javax.annotation.Nullable;

import static org.cloudburstmc.server.item.TierTypes.*;
import static org.cloudburstmc.server.item.ToolTypes.*;

public class ItemTypes {

    private static final Reference2ReferenceMap<Identifier, ItemType> BY_ID = new Reference2ReferenceOpenHashMap<>();

    public static final ItemType IRON_SHOVEL = IntItem.builder().id(ItemIds.IRON_SHOVEL).maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(IRON).attackDamage(4).build();
    public static final ItemType IRON_PICKAXE = IntItem.builder().id(ItemIds.IRON_PICKAXE).maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(IRON).attackDamage(4).build();
    public static final ItemType IRON_AXE = IntItem.builder().id(ItemIds.IRON_AXE).maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(IRON).attackDamage(6).build();
    public static final ItemType FLINT_AND_STEEL = IntItem.builder().id(ItemIds.FLINT_AND_STEEL).maxStackSize(1).build();
    public static final ItemType APPLE = IntItem.builder().id(ItemIds.APPLE).maxStackSize(64).build();
    public static final ItemType BOW = IntItem.builder().id(ItemIds.BOW).maxStackSize(1).fuelTime(200).build();
    public static final ItemType ARROW = IntItem.builder().id(ItemIds.ARROW).maxStackSize(64).build();
    public static final ItemType COAL = IntItem.builder().id(ItemIds.COAL).maxStackSize(64).data(Coal.class).fuelTime(1600).build();
    public static final ItemType DIAMOND = IntItem.builder().id(ItemIds.DIAMOND).maxStackSize(64).build();
    public static final ItemType IRON_INGOT = IntItem.builder().id(ItemIds.IRON_INGOT).maxStackSize(64).build();
    public static final ItemType GOLD_INGOT = IntItem.builder().id(ItemIds.GOLD_INGOT).maxStackSize(64).build();
    public static final ItemType IRON_SWORD = IntItem.builder().id(ItemIds.IRON_SWORD).maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(IRON).attackDamage(7).build();
    public static final ItemType WOODEN_SWORD = IntItem.builder().id(ItemIds.WOODEN_SWORD).maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(WOOD).attackDamage(5).fuelTime(200).build();
    public static final ItemType WOODEN_SHOVEL = IntItem.builder().id(ItemIds.WOODEN_SHOVEL).maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(WOOD).attackDamage(2).fuelTime(200).build();
    public static final ItemType WOODEN_PICKAXE = IntItem.builder().id(ItemIds.WOODEN_PICKAXE).maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(WOOD).attackDamage(2).fuelTime(200).build();
    public static final ItemType WOODEN_AXE = IntItem.builder().id(ItemIds.WOODEN_AXE).maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(WOOD).attackDamage(4).fuelTime(200).build();
    public static final ItemType STONE_SWORD = IntItem.builder().id(ItemIds.STONE_SWORD).maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(STONE).attackDamage(6).build();
    public static final ItemType STONE_SHOVEL = IntItem.builder().id(ItemIds.STONE_SHOVEL).maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(STONE).attackDamage(3).build();
    public static final ItemType STONE_PICKAXE = IntItem.builder().id(ItemIds.STONE_PICKAXE).maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(STONE).attackDamage(3).build();
    public static final ItemType STONE_AXE = IntItem.builder().id(ItemIds.STONE_AXE).maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(STONE).attackDamage(5).build();
    public static final ItemType DIAMOND_SWORD = IntItem.builder().id(ItemIds.DIAMOND_SWORD).maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(TierTypes.DIAMOND).attackDamage(8).build();
    public static final ItemType DIAMOND_SHOVEL = IntItem.builder().id(ItemIds.DIAMOND_SHOVEL).maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(TierTypes.DIAMOND).attackDamage(5).build();
    public static final ItemType DIAMOND_PICKAXE = IntItem.builder().id(ItemIds.DIAMOND_PICKAXE).maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(TierTypes.DIAMOND).attackDamage(5).build();
    public static final ItemType DIAMOND_AXE = IntItem.builder().id(ItemIds.DIAMOND_AXE).maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(TierTypes.DIAMOND).attackDamage(7).build();
    public static final ItemType STICK = IntItem.builder().id(ItemIds.STICK).maxStackSize(64).fuelTime(100).build();
    public static final ItemType BOWL = IntItem.builder().id(ItemIds.BOWL).maxStackSize(64).fuelTime(200).build();
    public static final ItemType MUSHROOM_STEW = IntItem.builder().id(ItemIds.MUSHROOM_STEW).maxStackSize(1).build();
    public static final ItemType GOLDEN_SWORD = IntItem.builder().id(ItemIds.GOLDEN_SWORD).maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(GOLD).attackDamage(5).build();
    public static final ItemType GOLDEN_SHOVEL = IntItem.builder().id(ItemIds.GOLDEN_SHOVEL).maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(GOLD).attackDamage(2).build();
    public static final ItemType GOLDEN_PICKAXE = IntItem.builder().id(ItemIds.GOLDEN_PICKAXE).maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(GOLD).attackDamage(2).build();
    public static final ItemType GOLDEN_AXE = IntItem.builder().id(ItemIds.GOLDEN_AXE).maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(GOLD).attackDamage(4).build();
    public static final ItemType STRING = IntItem.builder().id(ItemIds.STRING).maxStackSize(64).blockType(BlockTypes.TRIPWIRE).build();
    public static final ItemType FEATHER = IntItem.builder().id(ItemIds.FEATHER).maxStackSize(64).build();
    public static final ItemType GUNPOWDER = IntItem.builder().id(ItemIds.GUNPOWDER).maxStackSize(64).build();
    public static final ItemType WOODEN_HOE = IntItem.builder().id(ItemIds.WOODEN_HOE).maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(WOOD).attackDamage(2).fuelTime(200).build();
    public static final ItemType STONE_HOE = IntItem.builder().id(ItemIds.STONE_HOE).maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(STONE).attackDamage(3).build();
    public static final ItemType IRON_HOE = IntItem.builder().id(ItemIds.IRON_HOE).maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(IRON).attackDamage(4).build();
    public static final ItemType DIAMOND_HOE = IntItem.builder().id(ItemIds.DIAMOND_HOE).maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(TierTypes.DIAMOND).attackDamage(5).build();
    public static final ItemType GOLDEN_HOE = IntItem.builder().id(ItemIds.GOLDEN_HOE).maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(GOLD).attackDamage(2).build();
    public static final ItemType WHEAT_SEEDS = IntItem.builder().id(ItemIds.WHEAT_SEEDS).maxStackSize(64).blockType(BlockTypes.WHEAT).build();
    public static final ItemType WHEAT = IntItem.builder().id(ItemIds.WHEAT).maxStackSize(64).build();
    public static final ItemType BREAD = IntItem.builder().id(ItemIds.BREAD).maxStackSize(64).build();
    public static final ItemType LEATHER_HELMET = IntItem.builder().id(ItemIds.LEATHER_HELMET).maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType LEATHER_CHESTPLATE = IntItem.builder().id(ItemIds.LEATHER_CHESTPLATE).maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType LEATHER_LEGGINGS = IntItem.builder().id(ItemIds.LEATHER_LEGGINGS).maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType LEATHER_BOOTS = IntItem.builder().id(ItemIds.LEATHER_BOOTS).maxStackSize(1).tierType(TierTypes.LEATHER).build(); //TODO: color meta
    public static final ItemType CHAINMAIL_HELMET = IntItem.builder().id(ItemIds.CHAINMAIL_HELMET).maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType CHAINMAIL_CHESTPLATE = IntItem.builder().id(ItemIds.CHAINMAIL_CHESTPLATE).maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType CHAINMAIL_LEGGINGS = IntItem.builder().id(ItemIds.CHAINMAIL_LEGGINGS).maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType CHAINMAIL_BOOTS = IntItem.builder().id(ItemIds.CHAINMAIL_BOOTS).maxStackSize(1).tierType(CHAINMAIL).build();
    public static final ItemType IRON_HELMET = IntItem.builder().id(ItemIds.IRON_HELMET).maxStackSize(1).tierType(IRON).build();
    public static final ItemType IRON_CHESTPLATE = IntItem.builder().id(ItemIds.IRON_CHESTPLATE).maxStackSize(1).tierType(IRON).build();
    public static final ItemType IRON_LEGGINGS = IntItem.builder().id(ItemIds.IRON_LEGGINGS).maxStackSize(1).tierType(IRON).build();
    public static final ItemType IRON_BOOTS = IntItem.builder().id(ItemIds.IRON_BOOTS).maxStackSize(1).tierType(IRON).build();
    public static final ItemType DIAMOND_HELMET = IntItem.builder().id(ItemIds.DIAMOND_HELMET).maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType DIAMOND_CHESTPLATE = IntItem.builder().id(ItemIds.DIAMOND_CHESTPLATE).maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType DIAMOND_LEGGINGS = IntItem.builder().id(ItemIds.DIAMOND_LEGGINGS).maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType DIAMOND_BOOTS = IntItem.builder().id(ItemIds.DIAMOND_BOOTS).maxStackSize(1).tierType(TierTypes.DIAMOND).build();
    public static final ItemType GOLDEN_HELMET = IntItem.builder().id(ItemIds.GOLDEN_HELMET).maxStackSize(1).tierType(GOLD).build();
    public static final ItemType GOLDEN_CHESTPLATE = IntItem.builder().id(ItemIds.GOLDEN_CHESTPLATE).maxStackSize(1).tierType(GOLD).build();
    public static final ItemType GOLDEN_LEGGINGS = IntItem.builder().id(ItemIds.GOLDEN_LEGGINGS).maxStackSize(1).tierType(GOLD).build();
    public static final ItemType GOLDEN_BOOTS = IntItem.builder().id(ItemIds.GOLDEN_BOOTS).maxStackSize(1).tierType(GOLD).build();
    public static final ItemType FLINT = IntItem.builder().id(ItemIds.FLINT).maxStackSize(64).build();
    public static final ItemType PORKCHOP = IntItem.builder().id(ItemIds.PORKCHOP).maxStackSize(64).build();
    public static final ItemType COOKED_PORKCHOP = IntItem.builder().id(ItemIds.COOKED_PORKCHOP).maxStackSize(64).build();
    public static final ItemType PAINTING = IntItem.builder().id(ItemIds.PAINTING).maxStackSize(64).build();
    public static final ItemType GOLDEN_APPLE = IntItem.builder().id(ItemIds.GOLDEN_APPLE).maxStackSize(64).build();
    public static final ItemType SIGN = IntItem.builder().id(ItemIds.SIGN).maxStackSize(16).blockType(BlockTypes.WOODEN_STANDING_SIGN).data(TreeSpecies.class).build(); //TODO: wood types
    public static final ItemType WOODEN_DOOR = IntItem.builder().id(ItemIds.WOODEN_DOOR).maxStackSize(64).blockType(BlockTypes.WOODEN_DOOR).build(); //TODO: wood types
    public static final ItemType BUCKET = IntItem.builder().id(ItemIds.BUCKET).data(Bucket.class).maxStackSize(16).build();
    public static final ItemType MINECART = IntItem.builder().id(ItemIds.MINECART).maxStackSize(1).build();
    public static final ItemType SADDLE = IntItem.builder().id(ItemIds.SADDLE).maxStackSize(1).build();
    public static final ItemType IRON_DOOR = IntItem.builder().id(ItemIds.IRON_DOOR).maxStackSize(64).blockType(BlockTypes.IRON_DOOR).build();
    public static final ItemType REDSTONE = IntItem.builder().id(ItemIds.REDSTONE).maxStackSize(64).blockType(BlockTypes.REDSTONE_WIRE).build();
    public static final ItemType SNOWBALL = IntItem.builder().id(ItemIds.SNOWBALL).maxStackSize(16).build();
    public static final ItemType BOAT = IntItem.builder().id(ItemIds.BOAT).maxStackSize(1).fuelTime(1200).build();
    public static final ItemType LEATHER = IntItem.builder().id(ItemIds.LEATHER).maxStackSize(64).build();
    public static final ItemType KELP = IntItem.builder().id(ItemIds.KELP).maxStackSize(64).blockType(BlockTypes.KELP).build();
    public static final ItemType BRICK = IntItem.builder().id(ItemIds.BRICK).maxStackSize(64).build();
    public static final ItemType CLAY_BALL = IntItem.builder().id(ItemIds.CLAY_BALL).maxStackSize(64).build();
    public static final ItemType REEDS = IntItem.builder().id(ItemIds.REEDS).maxStackSize(64).blockType(BlockTypes.REEDS).build();
    public static final ItemType PAPER = IntItem.builder().id(ItemIds.PAPER).maxStackSize(64).build();
    public static final ItemType BOOK = IntItem.builder().id(ItemIds.BOOK).maxStackSize(64).build();
    public static final ItemType SLIME_BALL = IntItem.builder().id(ItemIds.SLIME_BALL).maxStackSize(64).build();
    public static final ItemType CHEST_MINECART = IntItem.builder().id(ItemIds.CHEST_MINECART).maxStackSize(1).build();
    public static final ItemType EGG = IntItem.builder().id(ItemIds.EGG).maxStackSize(16).build();
    public static final ItemType COMPASS = IntItem.builder().id(ItemIds.COMPASS).maxStackSize(64).build();
    public static final ItemType FISHING_ROD = IntItem.builder().id(ItemIds.FISHING_ROD).maxStackSize(1).fuelTime(300).build();
    public static final ItemType CLOCK = IntItem.builder().id(ItemIds.CLOCK).maxStackSize(64).build();
    public static final ItemType GLOWSTONE_DUST = IntItem.builder().id(ItemIds.GLOWSTONE_DUST).maxStackSize(64).build();
    public static final ItemType FISH = IntItem.builder().id(ItemIds.FISH).maxStackSize(64).build();
    public static final ItemType COOKED_FISH = IntItem.builder().id(ItemIds.COOKED_FISH).maxStackSize(64).build();
    public static final ItemType DYE = IntItem.builder().id(ItemIds.DYE).maxStackSize(64).data(DyeColor.class).build();
    public static final ItemType BONE = IntItem.builder().id(ItemIds.BONE).maxStackSize(64).build();
    public static final ItemType SUGAR = IntItem.builder().id(ItemIds.SUGAR).maxStackSize(64).build();
    public static final ItemType CAKE = IntItem.builder().id(ItemIds.CAKE).maxStackSize(1).build();
    public static final ItemType BED = IntItem.builder().id(ItemIds.BED).maxStackSize(1).data(DyeColor.class).build();
    public static final ItemType REPEATER = IntItem.builder().id(ItemIds.REPEATER).maxStackSize(64).blockType(BlockTypes.REPEATER).build();
    public static final ItemType COOKIE = IntItem.builder().id(ItemIds.COOKIE).maxStackSize(64).build();
    public static final ItemType MAP = IntItem.builder().id(ItemIds.MAP).maxStackSize(64).build();
    public static final ItemType SHEARS = IntItem.builder().id(ItemIds.SHEARS).maxStackSize(1).data(Damageable.class).toolType(ToolTypes.SHEARS).build();
    public static final ItemType MELON = IntItem.builder().id(ItemIds.MELON).maxStackSize(64).build();
    public static final ItemType PUMPKIN_SEEDS = IntItem.builder().id(ItemIds.PUMPKIN_SEEDS).maxStackSize(64).blockType(BlockTypes.PUMPKIN_STEM).build();
    public static final ItemType MELON_SEEDS = IntItem.builder().id(ItemIds.MELON_SEEDS).maxStackSize(64).blockType(BlockTypes.MELON_STEM).build();
    public static final ItemType BEEF = IntItem.builder().id(ItemIds.BEEF).maxStackSize(64).build();
    public static final ItemType COOKED_BEEF = IntItem.builder().id(ItemIds.COOKED_BEEF).maxStackSize(64).build();
    public static final ItemType CHICKEN = IntItem.builder().id(ItemIds.CHICKEN).maxStackSize(64).build();
    public static final ItemType COOKED_CHICKEN = IntItem.builder().id(ItemIds.COOKED_CHICKEN).maxStackSize(64).build();
    public static final ItemType ROTTEN_FLESH = IntItem.builder().id(ItemIds.ROTTEN_FLESH).maxStackSize(64).build();
    public static final ItemType ENDER_PEARL = IntItem.builder().id(ItemIds.ENDER_PEARL).maxStackSize(64).build();
    public static final ItemType BLAZE_ROD = IntItem.builder().id(ItemIds.BLAZE_ROD).maxStackSize(64).fuelTime(2400).build();
    public static final ItemType GHAST_TEAR = IntItem.builder().id(ItemIds.GHAST_TEAR).maxStackSize(64).build();
    public static final ItemType GOLD_NUGGET = IntItem.builder().id(ItemIds.GOLD_NUGGET).maxStackSize(64).build();
    public static final ItemType NETHER_WART = IntItem.builder().id(ItemIds.NETHER_WART).maxStackSize(64).blockType(BlockTypes.NETHER_WART).build();
    public static final ItemType POTION = IntItem.builder().id(ItemIds.POTION).maxStackSize(1).build();
    public static final ItemType GLASS_BOTTLE = IntItem.builder().id(ItemIds.GLASS_BOTTLE).maxStackSize(64).build();
    public static final ItemType SPIDER_EYE = IntItem.builder().id(ItemIds.SPIDER_EYE).maxStackSize(64).build();
    public static final ItemType FERMENTED_SPIDER_EYE = IntItem.builder().id(ItemIds.FERMENTED_SPIDER_EYE).maxStackSize(64).build();
    public static final ItemType BLAZE_POWDER = IntItem.builder().id(ItemIds.BLAZE_POWDER).maxStackSize(64).build();
    public static final ItemType MAGMA_CREAM = IntItem.builder().id(ItemIds.MAGMA_CREAM).maxStackSize(64).build();
    public static final ItemType BREWING_STAND = IntItem.builder().id(ItemIds.BREWING_STAND).maxStackSize(64).blockType(BlockTypes.BREWING_STAND).build();
    public static final ItemType CAULDRON = IntItem.builder().id(ItemIds.CAULDRON).maxStackSize(64).blockType(BlockTypes.CAULDRON).build();
    public static final ItemType ENDER_EYE = IntItem.builder().id(ItemIds.ENDER_EYE).maxStackSize(64).build();
    public static final ItemType SPECKLED_MELON = IntItem.builder().id(ItemIds.SPECKLED_MELON).maxStackSize(64).build();
    public static final ItemType SPAWN_EGG = IntItem.builder().id(ItemIds.SPAWN_EGG).maxStackSize(64).build();
    public static final ItemType EXPERIENCE_BOTTLE = IntItem.builder().id(ItemIds.EXPERIENCE_BOTTLE).maxStackSize(64).build();
    public static final ItemType FIREBALL = IntItem.builder().id(ItemIds.FIREBALL).maxStackSize(64).build();
    public static final ItemType WRITABLE_BOOK = IntItem.builder().id(ItemIds.WRITABLE_BOOK).maxStackSize(1).build();
    public static final ItemType WRITTEN_BOOK = IntItem.builder().id(ItemIds.WRITTEN_BOOK).maxStackSize(16).build();
    public static final ItemType EMERALD = IntItem.builder().id(ItemIds.EMERALD).maxStackSize(64).build();
    public static final ItemType FRAME = IntItem.builder().id(ItemIds.FRAME).maxStackSize(64).blockType(BlockTypes.FRAME).build();
    public static final ItemType FLOWER_POT = IntItem.builder().id(ItemIds.FLOWER_POT).maxStackSize(64).blockType(BlockTypes.FLOWER_POT).build();
    public static final ItemType CARROT = IntItem.builder().id(ItemIds.CARROT).maxStackSize(64).build();
    public static final ItemType POTATO = IntItem.builder().id(ItemIds.POTATO).maxStackSize(64).build();
    public static final ItemType BAKED_POTATO = IntItem.builder().id(ItemIds.BAKED_POTATO).maxStackSize(64).build();
    public static final ItemType POISONOUS_POTATO = IntItem.builder().id(ItemIds.POISONOUS_POTATO).maxStackSize(64).build();
    public static final ItemType EMPTY_MAP = IntItem.builder().id(ItemIds.EMPTY_MAP).maxStackSize(64).build();
    public static final ItemType GOLDEN_CARROT = IntItem.builder().id(ItemIds.GOLDEN_CARROT).maxStackSize(64).build();
    public static final ItemType SKULL = IntItem.builder().id(ItemIds.SKULL).maxStackSize(64).build(); //TODO: skull type
    public static final ItemType CARROT_ON_A_STICK = IntItem.builder().id(ItemIds.CARROT_ON_A_STICK).maxStackSize(1).build();
    public static final ItemType NETHER_STAR = IntItem.builder().id(ItemIds.NETHER_STAR).maxStackSize(64).build();
    public static final ItemType PUMPKIN_PIE = IntItem.builder().id(ItemIds.PUMPKIN_PIE).maxStackSize(64).build();
    public static final ItemType FIREWORKS = IntItem.builder().id(ItemIds.FIREWORKS).maxStackSize(64).build();
    public static final ItemType FIREWORKS_CHARGE = IntItem.builder().id(ItemIds.FIREWORKS_CHARGE).maxStackSize(64).build();
    public static final ItemType ENCHANTED_BOOK = IntItem.builder().id(ItemIds.ENCHANTED_BOOK).maxStackSize(1).build();
    public static final ItemType COMPARATOR = IntItem.builder().id(ItemIds.COMPARATOR).maxStackSize(64).blockType(BlockTypes.COMPARATOR).build();
    public static final ItemType NETHERBRICK = IntItem.builder().id(ItemIds.NETHERBRICK).maxStackSize(64).build();
    public static final ItemType QUARTZ = IntItem.builder().id(ItemIds.QUARTZ).maxStackSize(64).build();
    public static final ItemType TNT_MINECART = IntItem.builder().id(ItemIds.TNT_MINECART).maxStackSize(1).build();
    public static final ItemType HOPPER_MINECART = IntItem.builder().id(ItemIds.HOPPER_MINECART).maxStackSize(1).build();
    public static final ItemType PRISMARINE_SHARD = IntItem.builder().id(ItemIds.PRISMARINE_SHARD).maxStackSize(64).build();
    public static final ItemType HOPPER = IntItem.builder().id(ItemIds.HOPPER).maxStackSize(64).blockType(BlockTypes.HOPPER).build();
    public static final ItemType RABBIT = IntItem.builder().id(ItemIds.RABBIT).maxStackSize(64).build();
    public static final ItemType COOKED_RABBIT = IntItem.builder().id(ItemIds.COOKED_RABBIT).maxStackSize(64).build();
    public static final ItemType RABBIT_STEW = IntItem.builder().id(ItemIds.RABBIT_STEW).maxStackSize(64).build();
    public static final ItemType RABBIT_FOOT = IntItem.builder().id(ItemIds.RABBIT_FOOT).maxStackSize(64).build();
    public static final ItemType RABBIT_HIDE = IntItem.builder().id(ItemIds.RABBIT_HIDE).maxStackSize(64).build();
    public static final ItemType HORSE_ARMOR_LEATHER = IntItem.builder().id(ItemIds.HORSE_ARMOR_LEATHER).maxStackSize(1).build();
    public static final ItemType HORSE_ARMOR_IRON = IntItem.builder().id(ItemIds.HORSE_ARMOR_IRON).maxStackSize(1).build();
    public static final ItemType HORSE_ARMOR_GOLD = IntItem.builder().id(ItemIds.HORSE_ARMOR_GOLD).maxStackSize(1).build();
    public static final ItemType HORSE_ARMOR_DIAMOND = IntItem.builder().id(ItemIds.HORSE_ARMOR_DIAMOND).maxStackSize(1).build();
    public static final ItemType LEAD = IntItem.builder().id(ItemIds.LEAD).maxStackSize(64).build();
    public static final ItemType NAME_TAG = IntItem.builder().id(ItemIds.NAME_TAG).maxStackSize(64).build();
    public static final ItemType PRISMARINE_CRYSTALS = IntItem.builder().id(ItemIds.PRISMARINE_CRYSTALS).maxStackSize(64).build();
    public static final ItemType MUTTON_RAW = IntItem.builder().id(ItemIds.MUTTON_RAW).maxStackSize(64).build();
    public static final ItemType MUTTON_COOKED = IntItem.builder().id(ItemIds.MUTTON_COOKED).maxStackSize(64).build();
    public static final ItemType ARMOR_STAND = IntItem.builder().id(ItemIds.ARMOR_STAND).maxStackSize(64).build();
    public static final ItemType END_CRYSTAL = IntItem.builder().id(ItemIds.END_CRYSTAL).maxStackSize(64).build();
    public static final ItemType CHORUS_FRUIT = IntItem.builder().id(ItemIds.CHORUS_FRUIT).maxStackSize(64).build();
    public static final ItemType CHORUS_FRUIT_POPPED = IntItem.builder().id(ItemIds.CHORUS_FRUIT_POPPED).maxStackSize(64).build();
    public static final ItemType DRAGON_BREATH = IntItem.builder().id(ItemIds.DRAGON_BREATH).maxStackSize(64).build();
    public static final ItemType SPLASH_POTION = IntItem.builder().id(ItemIds.SPLASH_POTION).maxStackSize(1).build();
    public static final ItemType LINGERING_POTION = IntItem.builder().id(ItemIds.LINGERING_POTION).maxStackSize(1).build();
    public static final ItemType COMMAND_BLOCK_MINECART = IntItem.builder().id(ItemIds.COMMAND_BLOCK_MINECART).maxStackSize(1).build();
    public static final ItemType ELYTRA = IntItem.builder().id(ItemIds.ELYTRA).maxStackSize(1).build();
    public static final ItemType SHULKER_SHELL = IntItem.builder().id(ItemIds.SHULKER_SHELL).maxStackSize(64).build();
    public static final ItemType BANNER = IntItem.builder().id(ItemIds.BANNER).maxStackSize(16).fuelTime(300).build();
    public static final ItemType TOTEM = IntItem.builder().id(ItemIds.TOTEM).maxStackSize(1).build();
    public static final ItemType IRON_NUGGET = IntItem.builder().id(ItemIds.IRON_NUGGET).maxStackSize(64).build();
    public static final ItemType BOARD = IntItem.builder().id(ItemIds.BOARD).maxStackSize(16).build();
    public static final ItemType PORTFOLIO = IntItem.builder().id(ItemIds.PORTFOLIO).maxStackSize(64).build();
    public static final ItemType TRIDENT = IntItem.builder().id(ItemIds.TRIDENT).maxStackSize(64).build();
    public static final ItemType BEETROOT = IntItem.builder().id(ItemIds.BEETROOT).maxStackSize(54).blockType(BlockTypes.BEETROOT).build();
    public static final ItemType BEETROOT_SEEDS = IntItem.builder().id(ItemIds.BEETROOT_SEEDS).maxStackSize(64).build();
    public static final ItemType BEETROOT_SOUP = IntItem.builder().id(ItemIds.BEETROOT_SOUP).maxStackSize(1).build();
    public static final ItemType SALMON = IntItem.builder().id(ItemIds.SALMON).maxStackSize(64).build();
    public static final ItemType CLOWNFISH = IntItem.builder().id(ItemIds.CLOWNFISH).maxStackSize(64).build();
    public static final ItemType PUFFERFISH = IntItem.builder().id(ItemIds.PUFFERFISH).maxStackSize(64).build();
    public static final ItemType COOKED_SALMON = IntItem.builder().id(ItemIds.COOKED_SALMON).maxStackSize(64).build();
    public static final ItemType DRIED_KELP = IntItem.builder().id(ItemIds.DRIED_KELP).maxStackSize(64).build();
    public static final ItemType APPLE_ENCHANTED = IntItem.builder().id(ItemIds.APPLE_ENCHANTED).maxStackSize(64).build();
    public static final ItemType TURTLE_HELMET = IntItem.builder().id(ItemIds.TURTLE_HELMET).maxStackSize(1).build();
    public static final ItemType SWEET_BERRIES = IntItem.builder().id(ItemIds.SWEET_BERRIES).maxStackSize(64).build();
    public static final ItemType CAMERA = IntItem.builder().id(ItemIds.CAMERA).maxStackSize(64).build();
    public static final ItemType RECORD = IntItem.builder().id(ItemIds.RECORD_13).maxStackSize(1).build(); //TODO: record types

    public static final ItemType SHIELD = IntItem.builder().id(ItemIds.SHIELD).maxStackSize(1).data(Damageable.class).build();
    public static final ItemType CAMPFIRE = IntItem.builder().id(ItemIds.CAMPFIRE).maxStackSize(1).blockType(BlockTypes.CAMPFIRE).build();
    public static final ItemType HONEYCOMB = IntItem.builder().id(ItemIds.HONEYCOMB).maxStackSize(64).build();
    public static final ItemType HONEY_BOTTLE = IntItem.builder().id(ItemIds.HONEY_BOTTLE).maxStackSize(16).build();
    public static final ItemType NETHERITE_SWORD = IntItem.builder().id(ItemIds.NETHERITE_SWORD).maxStackSize(1).data(Damageable.class).toolType(SWORD).tierType(NETHERITE).attackDamage(9).build();
    public static final ItemType NETHERITE_SHOVEL = IntItem.builder().id(ItemIds.NETHERITE_SHOVEL).maxStackSize(1).data(Damageable.class).toolType(SHOVEL).tierType(NETHERITE).attackDamage(6).build();
    public static final ItemType NETHERITE_PICKAXE = IntItem.builder().id(ItemIds.NETHERITE_PICKAXE).maxStackSize(1).data(Damageable.class).toolType(PICKAXE).tierType(NETHERITE).attackDamage(6).build();
    public static final ItemType NETHERITE_AXE = IntItem.builder().id(ItemIds.NETHERITE_AXE).maxStackSize(1).data(Damageable.class).toolType(AXE).tierType(NETHERITE).attackDamage(8).build();
    public static final ItemType NETHERITE_HOE = IntItem.builder().id(ItemIds.NETHERITE_HOE).maxStackSize(1).data(Damageable.class).toolType(HOE).tierType(NETHERITE).attackDamage(6).build();
    public static final ItemType NETHERITE_HELMET = IntItem.builder().id(ItemIds.NETHERITE_HELMET).maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType NETHERITE_CHESTPLATE = IntItem.builder().id(ItemIds.NETHERITE_CHESTPLATE).maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType NETHERITE_LEGGINGS = IntItem.builder().id(ItemIds.NETHERITE_LEGGINGS).maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();
    public static final ItemType NETHERITE_BOOTS = IntItem.builder().id(ItemIds.NETHERITE_BOOTS).maxStackSize(1).data(Damageable.class).tierType(NETHERITE).build();

    private static class IntItem implements ItemType {
        private final Identifier id;
        private final int maxStackSize;
        private final int attackDamage;
        private final int armorPoints;
        private final int toughness;
        private final int durability;
        private final int fuelTime;
        private final int enchantAbility;
        private final BlockType blockType;
        private final Class<?> data;
        private final ToolType toolType;
        private final TierType tierType;

        public IntItem(
                Identifier id,
                int maxStackSize,
                int attackDamage,
                int armorPoints,
                int toughness,
                int durability,
                int fuelTime,
                int enchantAbility,
                Class<?> data,
                BlockType blockType,
                ToolType toolType,
                TierType tierType
        ) {
            this.id = id;
            this.maxStackSize = Math.min(1, maxStackSize);
            this.attackDamage = attackDamage;
            this.armorPoints = armorPoints;
            this.toughness = toughness;
            this.fuelTime = fuelTime;
            this.enchantAbility = enchantAbility;
            this.data = data;
            this.blockType = blockType;
            this.toolType = toolType;
            this.tierType = tierType;

            if (durability >= 0) {
                this.durability = durability;
            } else {
                if (tierType != null) {
                    this.durability = tierType.getDurability();
                } else {
                    this.durability = 0;
                }
            }

            BY_ID.put(id, this);
        }

        @Override
        public Identifier getId() {
            return id;
        }

        @Override
        public boolean isBlock() {
            return false;
        }

        @Override
        public int getMaximumStackSize() {
            return maxStackSize;
        }

        @Override
        public int getAttackDamage() {
            return attackDamage;
        }

        @Override
        public int getArmorPoints() {
            return armorPoints;
        }

        @Override
        public int getToughness() {
            return toughness;
        }

        @Override
        public int getDurability() {
            return durability;
        }

        @Override
        public int getFuelTime() {
            return fuelTime;
        }

        public BlockType getBlockType() {
            return blockType;
        }

        @Nullable
        @Override
        public Class<?> getMetadataClass() {
            return data;
        }

        @Override
        @Nullable
        public ToolType getToolType() {
            return toolType;
        }

        @Override
        @Nullable
        public TierType getTierType() {
            return tierType;
        }

        @Override
        public boolean isPlaceable() {
            return blockType != null;
        }

        @Override
        @Nullable
        public BlockType getBlock() {
            return null;
        }

        @Override
        public String toString() {
            return "ItemType(" + id + ")";
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        private static Builder builder() {
            return new Builder();
        }

        private static class Builder {
            private Identifier id;
            private int maxStackSize;
            private int attackDamage = 2;
            private int armorPoints;
            private int toughness;
            private int durability = -1;
            private int fuelTime;
            private int enchantAbility;
            private BlockType blockType;
            private Class<?> data;
            private ToolType toolType;
            private TierType tierType;

            private Builder id(Identifier id) {
                this.id = id;
                return this;
            }

            private Builder maxStackSize(int maxStackSize) {
                this.maxStackSize = maxStackSize;
                return this;
            }

            private Builder attackDamage(int damage) {
                this.attackDamage = damage;
                return this;
            }

            private Builder armorPoints(int points) {
                this.armorPoints = points;
                return this;
            }

            private Builder toughness(int toughness) {
                this.toughness = toughness;
                return this;
            }

            private Builder durability(int durability) {
                this.durability = durability;
                return this;
            }

            private Builder fuelTime(int duration) {
                this.fuelTime = duration;
                return this;
            }

            private Builder enchantAbility(int enchantAbility) {
                this.enchantAbility = enchantAbility;
                return this;
            }

            private Builder blockType(BlockType type) {
                this.blockType = type;
                return this;
            }

            private Builder data(Class<?> data) {
                this.data = data;
                return this;
            }

            private Builder toolType(ToolType toolType) {
                this.data(Damageable.class).toolType = toolType;
                return this;
            }

            private Builder tierType(TierType tierType) {
                this.tierType = tierType;
                return this;
            }

            public IntItem build() {
                return new IntItem(
                        id,
                        maxStackSize,
                        attackDamage,
                        armorPoints,
                        toughness,
                        durability,
                        fuelTime,
                        enchantAbility,
                        data,
                        blockType,
                        toolType,
                        tierType
                );
            }
        }
    }

    public static ItemType byId(Identifier id) {
        return byId(id, false);
    }

    public static ItemType byId(Identifier id, boolean itemsOnly) {
        ItemType type = BY_ID.get(id);
        if (type == null) {
            if (itemsOnly) {
                throw new IllegalArgumentException("ID " + id + " is not valid.");
            } else {
                return BlockTypes.byId(id);
            }
        }
        return type;
    }
}
