package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Builder;
import org.cloudburstmc.server.utils.Identifier;

public class BlockTypes {

    private static final Reference2ReferenceMap<Identifier, BlockType> BY_ID = new Reference2ReferenceOpenHashMap<>();

    public static final BlockType AIR = IntBlock.builder().id(BlockIds.AIR).maxStackSize(0).transparent(true).solid(true).floodable(true).build();
    public static final BlockType STONE = IntBlock.builder().id(BlockIds.STONE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType GRASS = IntBlock.builder().id(BlockIds.GRASS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.6f).build();
    public static final BlockType DIRT = IntBlock.builder().id(BlockIds.DIRT).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType COBBLESTONE = IntBlock.builder().id(BlockIds.COBBLESTONE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType PLANKS = IntBlock.builder().id(BlockIds.PLANKS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).flammable(true).fuelTime(300).build(); //TODO: planks type (warped, crimson)
    public static final BlockType SAPLING = IntBlock.builder().id(BlockIds.SAPLING).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).fuelTime(100).build();
    public static final BlockType BEDROCK = IntBlock.builder().id(BlockIds.BEDROCK).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType FLOWING_WATER = IntBlock.builder().id(BlockIds.FLOWING_WATER).maxStackSize(0).transparent(true).solid(true).filterLight(2).hardness(100f).build();
    public static final BlockType WATER = IntBlock.builder().id(BlockIds.WATER).maxStackSize(0).transparent(true).solid(true).filterLight(2).hardness(100f).build();
    public static final BlockType FLOWING_LAVA = IntBlock.builder().id(BlockIds.FLOWING_LAVA).maxStackSize(0).transparent(true).solid(true).emitLight(15).hardness(100f).build();
    public static final BlockType LAVA = IntBlock.builder().id(BlockIds.LAVA).maxStackSize(0).transparent(true).solid(true).emitLight(15).hardness(100f).build();
    public static final BlockType SAND = IntBlock.builder().id(BlockIds.SAND).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType GRAVEL = IntBlock.builder().id(BlockIds.GRAVEL).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.6f).build();
    public static final BlockType GOLD_ORE = IntBlock.builder().id(BlockIds.GOLD_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType IRON_ORE = IntBlock.builder().id(BlockIds.IRON_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType COAL_ORE = IntBlock.builder().id(BlockIds.COAL_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType LOG = IntBlock.builder().id(BlockIds.LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).flammable(true).build(); //TODO: include warped and stripped logs?
    public static final BlockType LEAVES = IntBlock.builder().id(BlockIds.LEAVES).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.2f).build();
    public static final BlockType SPONGE = IntBlock.builder().id(BlockIds.SPONGE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.6f).build();
    public static final BlockType GLASS = IntBlock.builder().id(BlockIds.GLASS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.3f).build();
    public static final BlockType LAPIS_ORE = IntBlock.builder().id(BlockIds.LAPIS_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType LAPIS_BLOCK = IntBlock.builder().id(BlockIds.LAPIS_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType DISPENSER = IntBlock.builder().id(BlockIds.DISPENSER).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3.5f).build();
    public static final BlockType SANDSTONE = IntBlock.builder().id(BlockIds.SANDSTONE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.8f).build();
    public static final BlockType NOTEBLOCK = IntBlock.builder().id(BlockIds.NOTEBLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.8f).fuelTime(300).build();
    public static final BlockType BED = IntBlock.builder().id(BlockIds.BED).maxStackSize(1).diggable(true).transparent(true).solid(true).hardness(0.2f).floodable(true).build();
    public static final BlockType GOLDEN_RAIL = IntBlock.builder().id(BlockIds.GOLDEN_RAIL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.7f).floodable(true).build();
    public static final BlockType DETECTOR_RAIL = IntBlock.builder().id(BlockIds.DETECTOR_RAIL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.7f).floodable(true).build();
    public static final BlockType STICKY_PISTON = IntBlock.builder().id(BlockIds.STICKY_PISTON).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.7f).build();
    public static final BlockType WEB = IntBlock.builder().id(BlockIds.WEB).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(4f).floodable(true).build();
    public static final BlockType TALL_GRASS = IntBlock.builder().id(BlockIds.TALL_GRASS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType DEADBUSH = IntBlock.builder().id(BlockIds.DEADBUSH).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType PISTON = IntBlock.builder().id(BlockIds.PISTON).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).build();
    public static final BlockType PISTON_ARM_COLLISION = IntBlock.builder().id(BlockIds.PISTON_ARM_COLLISION).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).build();
    public static final BlockType WOOL = IntBlock.builder().id(BlockIds.WOOL).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.8f).flammable(true).build();
    public static final BlockType ELEMENT_0 = IntBlock.builder().id(BlockIds.ELEMENT_0).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType YELLOW_FLOWER = IntBlock.builder().id(BlockIds.YELLOW_FLOWER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType RED_FLOWER = IntBlock.builder().id(BlockIds.RED_FLOWER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType BROWN_MUSHROOM = IntBlock.builder().id(BlockIds.BROWN_MUSHROOM).maxStackSize(64).diggable(true).solid(true).emitLight(1).filterLight(15).hardness(0f).build();
    public static final BlockType RED_MUSHROOM = IntBlock.builder().id(BlockIds.RED_MUSHROOM).maxStackSize(64).diggable(true).solid(true).emitLight(1).filterLight(15).hardness(0f).floodable(true).build();
    public static final BlockType GOLD_BLOCK = IntBlock.builder().id(BlockIds.GOLD_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType IRON_BLOCK = IntBlock.builder().id(BlockIds.IRON_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(5f).build();
    public static final BlockType DOUBLE_STONE_SLAB = IntBlock.builder().id(BlockIds.DOUBLE_STONE_SLAB).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).build(); //TODO: stone types
    public static final BlockType STONE_SLAB = IntBlock.builder().id(BlockIds.STONE_SLAB).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).build(); //TODO: stone types
    public static final BlockType BRICK_BLOCK = IntBlock.builder().id(BlockIds.BRICK_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType TNT = IntBlock.builder().id(BlockIds.TNT).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType BOOKSHELF = IntBlock.builder().id(BlockIds.BOOKSHELF).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).fuelTime(300).build();
    public static final BlockType MOSSY_COBBLESTONE = IntBlock.builder().id(BlockIds.MOSSY_COBBLESTONE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType OBSIDIAN = IntBlock.builder().id(BlockIds.OBSIDIAN).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(50f).build();
    public static final BlockType TORCH = IntBlock.builder().id(BlockIds.TORCH).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(14).hardness(0f).floodable(true).build(); //TODO: soul type
    public static final BlockType FIRE = IntBlock.builder().id(BlockIds.FIRE).maxStackSize(0).diggable(true).transparent(true).solid(true).emitLight(15).hardness(0f).floodable(true).build(); //TODO: soul fire type
    public static final BlockType MOB_SPAWNER = IntBlock.builder().id(BlockIds.MOB_SPAWNER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType WOOD_STAIRS = IntBlock.builder().id(BlockIds.OAK_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(2f).flammable(true).fuelTime(300).build(); //TODO: wood types
    public static final BlockType CHEST = IntBlock.builder().id(BlockIds.CHEST).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2.5f).fuelTime(300).build();
    public static final BlockType REDSTONE_WIRE = IntBlock.builder().id(BlockIds.REDSTONE_WIRE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType DIAMOND_ORE = IntBlock.builder().id(BlockIds.DIAMOND_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType DIAMOND_BLOCK = IntBlock.builder().id(BlockIds.DIAMOND_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(5f).build();
    public static final BlockType CRAFTING_TABLE = IntBlock.builder().id(BlockIds.CRAFTING_TABLE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2.5f).fuelTime(300).build();
    public static final BlockType WHEAT = IntBlock.builder().id(BlockIds.WHEAT).maxStackSize(0).diggable(true).solid(true).filterLight(15).hardness(0f).floodable(true).build();
    public static final BlockType FARMLAND = IntBlock.builder().id(BlockIds.FARMLAND).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(0.6f).build();
    public static final BlockType FURNACE = IntBlock.builder().id(BlockIds.FURNACE).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(13).hardness(3.5f).build();
    public static final BlockType LIT_FURNACE = IntBlock.builder().id(BlockIds.LIT_FURNACE).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(13).hardness(3.5f).build();
    public static final BlockType STANDING_SIGN = IntBlock.builder().id(BlockIds.STANDING_SIGN).maxStackSize(16).diggable(true).transparent(true).solid(true).hardness(1f).build(); //TODO: wood types
    public static final BlockType WOODEN_DOOR = IntBlock.builder().id(BlockIds.WOODEN_DOOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).flammable(true).fuelTime(200).build(); //TODO: wood types
    public static final BlockType LADDER = IntBlock.builder().id(BlockIds.LADDER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.4f).fuelTime(300).build();
    public static final BlockType RAIL = IntBlock.builder().id(BlockIds.RAIL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.7f).floodable(true).build();
    public static final BlockType STONE_STAIRS = IntBlock.builder().id(BlockIds.STONE_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).filterLight(15).build(); //TODO: stone types
    public static final BlockType WALL_SIGN = IntBlock.builder().id(BlockIds.WALL_SIGN).maxStackSize(16).diggable(true).transparent(true).solid(true).hardness(1f).build(); //TODO: wood types
    public static final BlockType LEVER = IntBlock.builder().id(BlockIds.LEVER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).floodable(true).build();
    public static final BlockType STONE_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.STONE_PRESSURE_PLATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).build(); //TODO: blackstone type
    public static final BlockType IRON_DOOR = IntBlock.builder().id(BlockIds.IRON_DOOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType WOODEN_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.WOODEN_PRESSURE_PLATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).fuelTime(300).build(); //TODO: wood types
    public static final BlockType REDSTONE_ORE = IntBlock.builder().id(BlockIds.REDSTONE_ORE).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(9).hardness(3f).build();
    public static final BlockType LIT_REDSTONE_ORE = IntBlock.builder().id(BlockIds.LIT_REDSTONE_ORE).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(9).hardness(3f).build();
    public static final BlockType REDSTONE_TORCH = IntBlock.builder().id(BlockIds.REDSTONE_TORCH).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(7).hardness(0f).floodable(true).build(); //TODO: unlit type
    public static final BlockType STONE_BUTTON = IntBlock.builder().id(BlockIds.STONE_BUTTON).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).floodable(true).build(); //TODO: blackstone
    public static final BlockType SNOW_LAYER = IntBlock.builder().id(BlockIds.SNOW_LAYER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.1f).floodable(true).build();
    public static final BlockType ICE = IntBlock.builder().id(BlockIds.ICE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).build();
    public static final BlockType SNOW = IntBlock.builder().id(BlockIds.SNOW).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.2f).build();
    public static final BlockType CACTUS = IntBlock.builder().id(BlockIds.CACTUS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.4f).build();
    public static final BlockType CLAY = IntBlock.builder().id(BlockIds.CLAY).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.6f).build();
    public static final BlockType REEDS = IntBlock.builder().id(BlockIds.REEDS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType JUKEBOX = IntBlock.builder().id(BlockIds.JUKEBOX).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType FENCE = IntBlock.builder().id(BlockIds.FENCE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).fuelTime(300).build(); //TODO: wood types
    public static final BlockType PUMPKIN = IntBlock.builder().id(BlockIds.PUMPKIN).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(1f).build();
    public static final BlockType NETHERRACK = IntBlock.builder().id(BlockIds.NETHERRACK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.4f).build();
    public static final BlockType SOUL_SAND = IntBlock.builder().id(BlockIds.SOUL_SAND).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType GLOWSTONE = IntBlock.builder().id(BlockIds.GLOWSTONE).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(15).hardness(0.3f).build();
    public static final BlockType PORTAL = IntBlock.builder().id(BlockIds.PORTAL).maxStackSize(0).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType LIT_PUMPKIN = IntBlock.builder().id(BlockIds.LIT_PUMPKIN).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(15).filterLight(15).hardness(1f).build();
    public static final BlockType CAKE = IntBlock.builder().id(BlockIds.CAKE).maxStackSize(1).diggable(true).transparent(true).solid(true).hardness(0.5f).build();
    public static final BlockType UNPOWERED_REPEATER = IntBlock.builder().id(BlockIds.UNPOWERED_REPEATER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType POWERED_REPEATER = IntBlock.builder().id(BlockIds.POWERED_REPEATER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType INVISIBLE_BEDROCK = IntBlock.builder().id(BlockIds.INVISIBLE_BEDROCK).maxStackSize(64).transparent(true).solid(true).hardness(-1f).build();
    public static final BlockType TRAPDOOR = IntBlock.builder().id(BlockIds.TRAPDOOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).fuelTime(300).build(); //TODO: wood types
    public static final BlockType MONSTER_EGG = IntBlock.builder().id(BlockIds.MONSTER_EGG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.75f).build();
    public static final BlockType STONEBRICK = IntBlock.builder().id(BlockIds.STONEBRICK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType BROWN_MUSHROOM_BLOCK = IntBlock.builder().id(BlockIds.BROWN_MUSHROOM_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.2f).fuelTime(300).build();
    public static final BlockType RED_MUSHROOM_BLOCK = IntBlock.builder().id(BlockIds.RED_MUSHROOM_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.2f).fuelTime(300).build();
    public static final BlockType IRON_BARS = IntBlock.builder().id(BlockIds.IRON_BARS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType GLASS_PANE = IntBlock.builder().id(BlockIds.GLASS_PANE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.3f).build();
    public static final BlockType MELON_BLOCK = IntBlock.builder().id(BlockIds.MELON_BLOCK).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(1f).build();
    public static final BlockType PUMPKIN_STEM = IntBlock.builder().id(BlockIds.PUMPKIN_STEM).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType MELON_STEM = IntBlock.builder().id(BlockIds.MELON_STEM).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType VINE = IntBlock.builder().id(BlockIds.VINE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.2f).floodable(true).build();
    public static final BlockType FENCE_GATE = IntBlock.builder().id(BlockIds.FENCE_GATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).fuelTime(300).build(); //TODO: wood types
    public static final BlockType BRICK_STAIRS = IntBlock.builder().id(BlockIds.BRICK_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType STONE_BRICK_STAIRS = IntBlock.builder().id(BlockIds.STONE_BRICK_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.5f).filterLight(15).build();
    public static final BlockType MYCELIUM = IntBlock.builder().id(BlockIds.MYCELIUM).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.6f).build();
    public static final BlockType WATERLILY = IntBlock.builder().id(BlockIds.WATERLILY).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType NETHER_BRICK = IntBlock.builder().id(BlockIds.NETHER_BRICK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build(); //TODO: nether brick type red/cracked/chiseled
    public static final BlockType NETHER_BRICK_FENCE = IntBlock.builder().id(BlockIds.NETHER_BRICK_FENCE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).build();
    public static final BlockType NETHER_BRICK_STAIRS = IntBlock.builder().id(BlockIds.NETHER_BRICK_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType NETHER_WART = IntBlock.builder().id(BlockIds.NETHER_WART).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType ENCHANTING_TABLE = IntBlock.builder().id(BlockIds.ENCHANTING_TABLE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType BREWING_STAND = IntBlock.builder().id(BlockIds.BREWING_STAND).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(1).hardness(0.5f).build();
    public static final BlockType CAULDRON = IntBlock.builder().id(BlockIds.CAULDRON).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).build(); //TODO: lava
    public static final BlockType END_PORTAL = IntBlock.builder().id(BlockIds.END_PORTAL).maxStackSize(64).transparent(true).solid(true).emitLight(15).hardness(-1f).build();
    public static final BlockType END_PORTAL_FRAME = IntBlock.builder().id(BlockIds.END_PORTAL_FRAME).maxStackSize(64).transparent(true).solid(true).emitLight(1).hardness(-1f).build();
    public static final BlockType END_STONE = IntBlock.builder().id(BlockIds.END_STONE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType DRAGON_EGG = IntBlock.builder().id(BlockIds.DRAGON_EGG).maxStackSize(64).transparent(true).solid(true).emitLight(1).hardness(3f).build();
    public static final BlockType REDSTONE_LAMP = IntBlock.builder().id(BlockIds.REDSTONE_LAMP).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(15).hardness(0.3f).build();
    public static final BlockType LIT_REDSTONE_LAMP = IntBlock.builder().id(BlockIds.LIT_REDSTONE_LAMP).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(15).hardness(0.3f).build();
    public static final BlockType DROPPER = IntBlock.builder().id(BlockIds.DROPPER).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3.5f).build();
    public static final BlockType ACTIVATOR_RAIL = IntBlock.builder().id(BlockIds.ACTIVATOR_RAIL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.7f).floodable(true).build();
    public static final BlockType COCOA = IntBlock.builder().id(BlockIds.COCOA).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.2f).build();
    public static final BlockType SANDSTONE_STAIRS = IntBlock.builder().id(BlockIds.SANDSTONE_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(0.8f).build();
    public static final BlockType EMERALD_ORE = IntBlock.builder().id(BlockIds.EMERALD_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType ENDER_CHEST = IntBlock.builder().id(BlockIds.ENDER_CHEST).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(22.5f).build();
    public static final BlockType TRIPWIRE_HOOK = IntBlock.builder().id(BlockIds.TRIPWIRE_HOOK).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType TRIPWIRE = IntBlock.builder().id(BlockIds.TRIPWIRE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType EMERALD_BLOCK = IntBlock.builder().id(BlockIds.EMERALD_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(5f).build();
    public static final BlockType COMMAND_BLOCK = IntBlock.builder().id(BlockIds.COMMAND_BLOCK).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType BEACON = IntBlock.builder().id(BlockIds.BEACON).maxStackSize(64).diggable(true).solid(true).emitLight(15).hardness(3f).build();
    public static final BlockType COBBLESTONE_WALL = IntBlock.builder().id(BlockIds.COBBLESTONE_WALL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).build(); //TODO: blackstone
    public static final BlockType FLOWER_POT = IntBlock.builder().id(BlockIds.FLOWER_POT).maxStackSize(64).diggable(true).transparent(true).solid(true).floodable(true).hardness(0f).build();
    public static final BlockType CARROTS = IntBlock.builder().id(BlockIds.CARROTS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0f).floodable(true).build();
    public static final BlockType POTATOES = IntBlock.builder().id(BlockIds.POTATOES).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0f).floodable(true).build();
    public static final BlockType WOODEN_BUTTON = IntBlock.builder().id(BlockIds.WOODEN_BUTTON).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).floodable(true).fuelTime(100).build(); //TODO: wood types
    public static final BlockType SKULL = IntBlock.builder().id(BlockIds.SKULL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1f).floodable(true).build();
    public static final BlockType ANVIL = IntBlock.builder().id(BlockIds.ANVIL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType TRAPPED_CHEST = IntBlock.builder().id(BlockIds.TRAPPED_CHEST).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2.5f).fuelTime(300).build();
    public static final BlockType LIGHT_WEIGHTED_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.LIGHT_WEIGHTED_PRESSURE_PLATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).build();
    public static final BlockType HEAVY_WEIGHTED_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.HEAVY_WEIGHTED_PRESSURE_PLATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).build();
    public static final BlockType UNPOWERED_COMPARATOR = IntBlock.builder().id(BlockIds.UNPOWERED_COMPARATOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType POWERED_COMPARATOR = IntBlock.builder().id(BlockIds.POWERED_COMPARATOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType DAYLIGHT_DETECTOR = IntBlock.builder().id(BlockIds.DAYLIGHT_DETECTOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.2f).fuelTime(300).build();
    public static final BlockType REDSTONE_BLOCK = IntBlock.builder().id(BlockIds.REDSTONE_BLOCK).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType QUARTZ_ORE = IntBlock.builder().id(BlockIds.QUARTZ_ORE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType HOPPER = IntBlock.builder().id(BlockIds.HOPPER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).build();
    public static final BlockType QUARTZ_BLOCK = IntBlock.builder().id(BlockIds.QUARTZ_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.8f).build(); //TODO: quartz bricks
    public static final BlockType QUARTZ_STAIRS = IntBlock.builder().id(BlockIds.QUARTZ_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(0.8f).build();
    public static final BlockType DOUBLE_WOODEN_SLAB = IntBlock.builder().id(BlockIds.DOUBLE_WOODEN_SLAB).maxStackSize(64).diggable(true).transparent(true).solid(true).flammable(true).hardness(2f).fuelTime(300).build(); //TODO: wood types
    public static final BlockType WOODEN_SLAB = IntBlock.builder().id(BlockIds.WOODEN_SLAB).maxStackSize(64).diggable(true).transparent(true).solid(true).flammable(true).hardness(2f).fuelTime(300).build(); //TODO: wood types
    public static final BlockType STAINED_HARDENED_CLAY = IntBlock.builder().id(BlockIds.STAINED_HARDENED_CLAY).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.25f).build();
    public static final BlockType STAINED_GLASS_PANE = IntBlock.builder().id(BlockIds.STAINED_GLASS_PANE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.3f).build();
    public static final BlockType SLIME = IntBlock.builder().id(BlockIds.SLIME).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    //    public static final BlockType GLOW_STICK = IntBlock.builder().id(BlockIds.GLOW_STICK).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(14).hardness(0f).floodable(true).build();
    public static final BlockType IRON_TRAPDOOR = IntBlock.builder().id(BlockIds.IRON_TRAPDOOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5f).build();
    public static final BlockType PRISMARINE = IntBlock.builder().id(BlockIds.PRISMARINE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType SEA_LANTERN = IntBlock.builder().id(BlockIds.SEA_LANTERN).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(15).hardness(0.3f).build();
    public static final BlockType HAY_BLOCK = IntBlock.builder().id(BlockIds.HAY_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType CARPET = IntBlock.builder().id(BlockIds.CARPET).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.1f).floodable(true).build();
    public static final BlockType HARDENED_CLAY = IntBlock.builder().id(BlockIds.HARDENED_CLAY).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.25f).build();
    public static final BlockType COAL_BLOCK = IntBlock.builder().id(BlockIds.COAL_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(5f).fuelTime(16000).build();
    public static final BlockType PACKED_ICE = IntBlock.builder().id(BlockIds.PACKED_ICE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType DOUBLE_PLANT = IntBlock.builder().id(BlockIds.DOUBLE_PLANT).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).floodable(true).build();
    public static final BlockType STANDING_BANNER = IntBlock.builder().id(BlockIds.STANDING_BANNER).maxStackSize(16).diggable(true).transparent(true).solid(true).hardness(1f).build();
    public static final BlockType WALL_BANNER = IntBlock.builder().id(BlockIds.WALL_BANNER).maxStackSize(16).diggable(true).transparent(true).solid(true).hardness(1f).build();
    public static final BlockType DAYLIGHT_DETECTOR_INVERTED = IntBlock.builder().id(BlockIds.DAYLIGHT_DETECTOR_INVERTED).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.2f).fuelTime(300).build();
    public static final BlockType RED_SANDSTONE = IntBlock.builder().id(BlockIds.RED_SANDSTONE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.8f).build();
    public static final BlockType RED_SANDSTONE_STAIRS = IntBlock.builder().id(BlockIds.RED_SANDSTONE_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(0.8f).build();
    public static final BlockType REPEATING_COMMAND_BLOCK = IntBlock.builder().id(BlockIds.REPEATING_COMMAND_BLOCK).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType CHAIN_COMMAND_BLOCK = IntBlock.builder().id(BlockIds.CHAIN_COMMAND_BLOCK).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType HARD_GLASS_PANE = IntBlock.builder().id(BlockIds.HARD_GLASS_PANE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.8f).build();
    public static final BlockType HARD_STAINED_GLASS_PANE = IntBlock.builder().id(BlockIds.HARD_STAINED_GLASS_PANE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.8f).build();
    public static final BlockType CHEMICAL_HEAT = IntBlock.builder().id(BlockIds.CHEMICAL_HEAT).maxStackSize(64).diggable(false).transparent(true).build();
    public static final BlockType GRASS_PATH = IntBlock.builder().id(BlockIds.GRASS_PATH).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.65f).build();
    public static final BlockType FRAME = IntBlock.builder().id(BlockIds.FRAME).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0f).floodable(true).build();
    public static final BlockType CHORUS_FLOWER = IntBlock.builder().id(BlockIds.CHORUS_FLOWER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.4f).build();
    public static final BlockType PURPUR_BLOCK = IntBlock.builder().id(BlockIds.PURPUR_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType COLORED_TORCH_RG = IntBlock.builder().id(BlockIds.COLORED_TORCH_RG).maxStackSize(64).diggable(true).solid(true).transparent(true).build();
    public static final BlockType PURPUR_STAIRS = IntBlock.builder().id(BlockIds.PURPUR_STAIRS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.5f).build();
    public static final BlockType COLORED_TORCH_BP = IntBlock.builder().id(BlockIds.COLORED_TORCH_BP).maxStackSize(64).diggable(true).solid(true).transparent(true).build();
    public static final BlockType UNDYED_SHULKER_BOX = IntBlock.builder().id(BlockIds.UNDYED_SHULKER_BOX).maxStackSize(1).diggable(true).transparent(true).solid(true).hardness(2f).build();
    public static final BlockType END_BRICKS = IntBlock.builder().id(BlockIds.END_BRICKS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType FROSTED_ICE = IntBlock.builder().id(BlockIds.FROSTED_ICE).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(2).hardness(0.5f).build();
    public static final BlockType END_ROD = IntBlock.builder().id(BlockIds.END_ROD).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(14).hardness(0f).floodable(true).build();
    public static final BlockType END_GATEWAY = IntBlock.builder().id(BlockIds.END_GATEWAY).maxStackSize(64).solid(true).emitLight(15).filterLight(15).hardness(-1f).build();
    public static final BlockType ALLOW = IntBlock.builder().id(BlockIds.ALLOW).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType DENY = IntBlock.builder().id(BlockIds.DENY).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType BORDER_BLOCK = IntBlock.builder().id(BlockIds.BORDER_BLOCK).maxStackSize(64).transparent(true).solid(true).hardness(-1f).build();
    public static final BlockType MAGMA = IntBlock.builder().id(BlockIds.MAGMA).maxStackSize(64).diggable(true).solid(true).emitLight(3).filterLight(15).hardness(0.5f).build();
    public static final BlockType NETHER_WART_BLOCK = IntBlock.builder().id(BlockIds.NETHER_WART_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1f).build(); //TODO: warped type
    public static final BlockType RED_NETHER_BRICK = IntBlock.builder().id(BlockIds.RED_NETHER_BRICK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType BONE_BLOCK = IntBlock.builder().id(BlockIds.BONE_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).build();
    public static final BlockType SHULKER_BOX = IntBlock.builder().id(BlockIds.SHULKER_BOX).maxStackSize(1).diggable(true).transparent(true).solid(true).hardness(2f).build();
    public static final BlockType PURPLE_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.PURPLE_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType WHITE_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.WHITE_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType ORANGE_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.ORANGE_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType MAGENTA_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.MAGENTA_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType LIGHT_BLUE_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.LIGHT_BLUE_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType YELLOW_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.YELLOW_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType LIME_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.LIME_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType PINK_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.PINK_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType GRAY_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.GRAY_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType SILVER_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.SILVER_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType CYAN_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.CYAN_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    //    public static final BlockType CHALKBOARD = IntBlock.builder().id(BlockIds.CHALKBOARD).maxStackSize(16).transparent(true).solid(true).hardness(-1f).build();
    public static final BlockType BLUE_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.BLUE_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType BROWN_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.BROWN_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType GREEN_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.GREEN_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType RED_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.RED_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType BLACK_GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.BLACK_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.4f).build();
    public static final BlockType CONCRETE = IntBlock.builder().id(BlockIds.CONCRETE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.8f).build();
    public static final BlockType CONCRETE_POWDER = IntBlock.builder().id(BlockIds.CONCRETE_POWDER).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType CHEMISTRY_TABLE = IntBlock.builder().id(BlockIds.CHEMISTRY_TABLE).maxStackSize(64).diggable(true).solid(true).hardness(0.5f).build();
    public static final BlockType UNDERWATER_TORCH = IntBlock.builder().id(BlockIds.UNDERWATER_TORCH).maxStackSize(64).diggable(true).solid(true).transparent(true).build();
    public static final BlockType CHORUS_PLANT = IntBlock.builder().id(BlockIds.CHORUS_PLANT).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.4f).build();
    public static final BlockType STAINED_GLASS = IntBlock.builder().id(BlockIds.STAINED_GLASS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.3f).build();
    public static final BlockType CAMERA = IntBlock.builder().id(BlockIds.CAMERA).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType PODZOL = IntBlock.builder().id(BlockIds.PODZOL).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
    public static final BlockType BEETROOT = IntBlock.builder().id(BlockIds.BEETROOT).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType STONECUTTER = IntBlock.builder().id(BlockIds.STONECUTTER).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3.5f).build();
    public static final BlockType GLOWING_OBSIDIAN = IntBlock.builder().id(BlockIds.GLOWING_OBSIDIAN).maxStackSize(64).diggable(true).solid(true).emitLight(12).filterLight(15).hardness(50f).build();
    public static final BlockType NETHER_REACTOR = IntBlock.builder().id(BlockIds.NETHER_REACTOR).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(3f).build();
    public static final BlockType INFO_UPDATE = IntBlock.builder().id(BlockIds.INFO_UPDATE).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0f).build();
    public static final BlockType INFO_UPDATE2 = IntBlock.builder().id(BlockIds.INFO_UPDATE2).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0f).build();
    public static final BlockType MOVING_BLOCK = IntBlock.builder().id(BlockIds.MOVING_BLOCK).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType OBSERVER = IntBlock.builder().id(BlockIds.OBSERVER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).build();
    public static final BlockType STRUCTURE_BLOCK = IntBlock.builder().id(BlockIds.STRUCTURE_BLOCK).maxStackSize(64).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType HARD_GLASS = IntBlock.builder().id(BlockIds.HARD_GLASS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.8f).build();
    public static final BlockType HARD_STAINED_GLASS = IntBlock.builder().id(BlockIds.HARD_STAINED_GLASS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.8f).build();
    public static final BlockType RESERVED6 = IntBlock.builder().id(BlockIds.RESERVED6).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(-1f).build();
    public static final BlockType PRISMARINE_STAIRS = IntBlock.builder().id(BlockIds.PRISMARINE_STAIRS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType DARK_PRISMARINE_STAIRS = IntBlock.builder().id(BlockIds.DARK_PRISMARINE_STAIRS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType PRISMARINE_BRICKS_STAIRS = IntBlock.builder().id(BlockIds.PRISMARINE_BRICKS_STAIRS).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType STRIPPED_SPRUCE_LOG = IntBlock.builder().id(BlockIds.STRIPPED_SPRUCE_LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType STRIPPED_BIRCH_LOG = IntBlock.builder().id(BlockIds.STRIPPED_BIRCH_LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType STRIPPED_JUNGLE_LOG = IntBlock.builder().id(BlockIds.STRIPPED_JUNGLE_LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType STRIPPED_ACACIA_LOG = IntBlock.builder().id(BlockIds.STRIPPED_ACACIA_LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType STRIPPED_DARK_OAK_LOG = IntBlock.builder().id(BlockIds.STRIPPED_DARK_OAK_LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType STRIPPED_OAK_LOG = IntBlock.builder().id(BlockIds.STRIPPED_OAK_LOG).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(2f).fuelTime(300).build();
    public static final BlockType BLUE_ICE = IntBlock.builder().id(BlockIds.BLUE_ICE).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(2).hardness(0.5f).build();

    static { // Lazy
        for (int i = 1; i <= 118; i++) {
            IntBlock.builder().id(Identifier.fromString("element_" + i)).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(0.5f).build();
        }
    }

    public static final BlockType SEAGRASS = IntBlock.builder().id(BlockIds.SEAGRASS).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType CORAL = IntBlock.builder().id(BlockIds.CORAL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.5f).build();
    public static final BlockType CORAL_BLOCK = IntBlock.builder().id(BlockIds.CORAL_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).build();
    public static final BlockType CORAL_FAN = IntBlock.builder().id(BlockIds.CORAL_FAN).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.5f).build();
    public static final BlockType CORAL_FAN_DEAD = IntBlock.builder().id(BlockIds.CORAL_FAN_DEAD).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.5f).build();
    public static final BlockType CORAL_FAN_HANG = IntBlock.builder().id(BlockIds.CORAL_FAN_HANG).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1.5f).build(); //TODO: 2, 3
    public static final BlockType KELP = IntBlock.builder().id(BlockIds.KELP).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType DRIED_KELP_BLOCK = IntBlock.builder().id(BlockIds.DRIED_KELP_BLOCK).maxStackSize(64).diggable(true).solid(true).filterLight(15).hardness(1.5f).fuelTime(4000).build();
    public static final BlockType CARVED_PUMPKIN = IntBlock.builder().id(BlockIds.CARVED_PUMPKIN).maxStackSize(64).diggable(true).transparent(true).solid(true).filterLight(15).hardness(1f).build();
    public static final BlockType SEA_PICKLE = IntBlock.builder().id(BlockIds.SEA_PICKLE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType CONDUIT = IntBlock.builder().id(BlockIds.CONDUIT).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).build();

    public static final BlockType TURTLE_EGG = IntBlock.builder().id(BlockIds.TURTLE_EGG).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0f).build();
    public static final BlockType BUBBLE_COLUMN = IntBlock.builder().id(BlockIds.BUBBLE_COLUMN).maxStackSize(64).diggable(false).transparent(true).solid(false).hardness(0f).build();
    public static final BlockType BARRIER = IntBlock.builder().id(BlockIds.BARRIER).maxStackSize(64).diggable(false).transparent(true).solid(true).hardness(-1f).build();
    public static final BlockType BAMBOO = IntBlock.builder().id(BlockIds.BAMBOO).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(1f).build();
    public static final BlockType BAMBOO_SAPLING = IntBlock.builder().id(BlockIds.BAMBOO_SAPLING).maxStackSize(64).diggable(true).transparent(true).hardness(1f).build();
    public static final BlockType SCAFFOLDING = IntBlock.builder().id(BlockIds.SCAFFOLDING).maxStackSize(64).diggable(true).transparent(true).solid(true).flammable(true).fuelTime(1200).build();
    public static final BlockType LECTERN = IntBlock.builder().id(BlockIds.LECTERN).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2.5f).resistance(2.5f).flammable(true).fuelTime(300).build();
    public static final BlockType GRINDSTONE = IntBlock.builder().id(BlockIds.GRINDSTONE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2).resistance(6).build();
    public static final BlockType BLAST_FURNACE = IntBlock.builder().id(BlockIds.BLAST_FURNACE).maxStackSize(64).diggable(true).solid(true).hardness(3.5f).resistance(3.5f).build(); //TODO: lit type
    public static final BlockType STONECUTTER_BLOCK = IntBlock.builder().id(BlockIds.STONECUTTER_BLOCK).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3.5f).resistance(3.5f).build();
    public static final BlockType SMOKER = IntBlock.builder().id(BlockIds.SMOKER).maxStackSize(64).diggable(true).solid(true).hardness(3.5f).resistance(3.5f).build();
    public static final BlockType CARTOGRAPHY_TABLE = IntBlock.builder().id(BlockIds.CARTOGRAPHY_TABLE).maxStackSize(64).diggable(true).solid(true).hardness(2.5f).resistance(2.5f).build();
    public static final BlockType FLETCHING_TABLE = IntBlock.builder().id(BlockIds.FLETCHING_TABLE).maxStackSize(64).diggable(true).solid(true).hardness(2.5f).resistance(2.5f).flammable(true).build();
    public static final BlockType SMITHING_TABLE = IntBlock.builder().id(BlockIds.SMITHING_TABLE).maxStackSize(64).diggable(true).solid(true).hardness(2.5f).resistance(2.5f).build();
    public static final BlockType BARREL = IntBlock.builder().id(BlockIds.BARREL).maxStackSize(64).diggable(true).solid(true).hardness(2.5f).resistance(2.5f).build();
    public static final BlockType LOOM = IntBlock.builder().id(BlockIds.LOOM).maxStackSize(64).diggable(true).solid(true).hardness(2.5f).resistance(2.5f).build();
    public static final BlockType BELL = IntBlock.builder().id(BlockIds.BELL).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(5).resistance(5).build();
    public static final BlockType SWEET_BERRY_BUSH = IntBlock.builder().id(BlockIds.SWEET_BERRY_BUSH).maxStackSize(64).diggable(true).transparent(true).flammable(true).build();
    public static final BlockType LANTERN = IntBlock.builder().id(BlockIds.LANTERN).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3.5f).resistance(3.5f).build(); //TODO: lantern type
    public static final BlockType CAMPFIRE = IntBlock.builder().id(BlockIds.CAMPFIRE).diggable(true).transparent(true).solid(true).hardness(2).resistance(2).build(); //TODO: campfire type
    public static final BlockType JIGSAW = IntBlock.builder().id(BlockIds.JIGSAW).maxStackSize(64).solid(true).resistance(3600000).build();
    public static final BlockType WOOD = IntBlock.builder().id(BlockIds.WOOD).maxStackSize(64).diggable(true).solid(true).hardness(2).resistance(2).flammable(true).build(); //TODO: properties per trait? //hyphae types
    public static final BlockType COMPOSTER = IntBlock.builder().id(BlockIds.COMPOSTER).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.6f).resistance(0.6f).flammable(true).build();
    public static final BlockType LIGHT_BLOCK = IntBlock.builder().id(BlockIds.LIGHT_BLOCK).maxStackSize(64).transparent(true).resistance(3600000).build();
    public static final BlockType BEE_NEST = IntBlock.builder().id(BlockIds.BEE_NEST).maxStackSize(64).diggable(true).solid(true).hardness(0.3f).resistance(0.3f).flammable(true).build();
    public static final BlockType BEEHIVE = IntBlock.builder().id(BlockIds.BEEHIVE).maxStackSize(64).diggable(true).solid(true).hardness(0.6f).resistance(0.6f).flammable(true).build();
    public static final BlockType HONEY_BLOCK = IntBlock.builder().id(BlockIds.HONEY_BLOCK).maxStackSize(64).diggable(true).transparent(true).solid(true).build();
    public static final BlockType HONEYCOMB_BLOCK = IntBlock.builder().id(BlockIds.HONEYCOMB_BLOCK).maxStackSize(64).diggable(true).solid(true).hardness(0.6f).resistance(0.6f).build();
    public static final BlockType LODESTONE = IntBlock.builder().id(BlockIds.LODESTONE).maxStackSize(64).diggable(true).solid(true).hardness(3.5f).resistance(3.5f).build();
    public static final BlockType CRIMSON_ROOTS = IntBlock.builder().id(BlockIds.CRIMSON_ROOTS).maxStackSize(64).diggable(true).transparent(true).build(); //TODO: roots type
    public static final BlockType CRIMSON_FUNGUS = IntBlock.builder().id(BlockIds.CRIMSON_FUNGUS).maxStackSize(64).diggable(true).transparent(true).build(); //TODO: fungus type
    public static final BlockType SHROOMLIGHT = IntBlock.builder().id(BlockIds.SHROOMLIGHT).maxStackSize(64).diggable(true).solid(true).hardness(1).resistance(1).emitLight(15).build();
    public static final BlockType WEEPING_VINES = IntBlock.builder().id(BlockIds.WEEPING_VINES).maxStackSize(64).diggable(true).transparent(true).build();
    public static final BlockType CRIMSON_NYLIUM = IntBlock.builder().id(BlockIds.CRIMSON_NYLIUM).maxStackSize(64).diggable(true).solid(true).hardness(1).resistance(1).build(); //TODO: nylium type
    public static final BlockType BASALT = IntBlock.builder().id(BlockIds.BASALT).maxStackSize(64).diggable(true).solid(true).hardness(1.25f).resistance(4.2f).build(); //TODO: polished type
    public static final BlockType SOUL_SOIL = IntBlock.builder().id(BlockIds.SOUL_SOIL).maxStackSize(64).diggable(true).solid(true).hardness(0.5f).resistance(0.5f).build();
    public static final BlockType NETHER_SPROUTS = IntBlock.builder().id(BlockIds.NETHER_SPROUTS).maxStackSize(64).diggable(true).transparent(true).build();
    public static final BlockType TARGET = IntBlock.builder().id(BlockIds.TARGET).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).resistance(0.5f).flammable(true).build();
    public static final BlockType NETHERITE_BLOCK = IntBlock.builder().id(BlockIds.NETHERITE_BLOCK).maxStackSize(64).diggable(true).solid(true).hardness(50).resistance(1200).build();
    public static final BlockType ANCIENT_DEBRIS = IntBlock.builder().id(BlockIds.ANCIENT_DEBRIS).maxStackSize(64).diggable(true).solid(true).hardness(30).resistance(1200).build();
    public static final BlockType RESPAWN_ANCHOR = IntBlock.builder().id(BlockIds.RESPAWN_ANCHOR).maxStackSize(64).diggable(true).solid(true).hardness(50).resistance(1200).build();
    public static final BlockType BLACKSTONE = IntBlock.builder().id(BlockIds.BLACKSTONE).maxStackSize(64).diggable(true).solid(true).hardness(1.5f).resistance(6).build();
    public static final BlockType POLISHED_BLACKSTONE_BRICKS = IntBlock.builder().id(BlockIds.POLISHED_BLACKSTONE_BRICKS).maxStackSize(64).diggable(true).solid(true).hardness(1.5f).resistance(6).build(); //TODO: cracked type
    public static final BlockType POLISHED_BLACKSTONE = IntBlock.builder().id(BlockIds.POLISHED_BLACKSTONE).maxStackSize(64).diggable(true).solid(true).resistance(6).build(); //TODO: chiseled type
    public static final BlockType GILDED_BLACKSTONE = IntBlock.builder().id(BlockIds.GILDED_BLACKSTONE).maxStackSize(64).diggable(true).solid(true).hardness(1.5f).resistance(6).build();
    public static final BlockType CHAIN = IntBlock.builder().id(BlockIds.CHAIN).maxStackSize(64).diggable(true).transparent(true).hardness(5).resistance(6).build();
    public static final BlockType TWISTING_VINES = IntBlock.builder().id(BlockIds.TWISTING_VINES).maxStackSize(64).diggable(true).transparent(true).build();
    public static final BlockType NETHER_GOLD_ORE = IntBlock.builder().id(BlockIds.NETHER_GOLD_ORE).maxStackSize(64).diggable(true).solid(true).hardness(3).resistance(3).build();
    public static final BlockType CRYING_OBSIDIAN = IntBlock.builder().id(BlockIds.CRYING_OBSIDIAN).maxStackSize(64).diggable(true).solid(true).hardness(50).resistance(1200).emitLight(10).build();

    public static BlockType byId(Identifier id) {
        BlockType type = BY_ID.get(id);
        if (type == null) {
            throw new IllegalArgumentException("ID " + id + " is not valid.");
        }
        return type;
    }

    @Builder
    private static class IntBlock implements BlockType {

        private final Identifier id;
        private final int maxStackSize;
        private final boolean diggable;
        private final boolean transparent;
        private final boolean flammable;
        private final boolean floodable;
        private final boolean solid;
        private final int emitLight;
        private final int filterLight;
        private final float hardness;
        private final int burnChance;
        private final int burnability;
        private final float resistance;
        private final int fuelTime;

        public IntBlock(
                Identifier id,
                int maxStackSize,
                boolean diggable,
                boolean transparent,
                boolean flammable,
                boolean floodable,
                boolean solid,
                int emitLight,
                int filterLight,
                float hardness,
                int burnChance,
                int burnability,
                float resistance,
                int fuelTime
        ) {
            this.id = id;
            this.maxStackSize = Math.max(1, maxStackSize);
            this.diggable = diggable;
            this.transparent = transparent;
            this.flammable = flammable;
            this.floodable = floodable;
            this.solid = solid;
            this.emitLight = emitLight;
            this.filterLight = filterLight;
            this.hardness = hardness;
            this.burnChance = burnChance;
            this.burnability = burnability;
            this.resistance = resistance;
            this.fuelTime = fuelTime;

            BY_ID.put(id, this);
        }

        @Override
        public Identifier getId() {
            return id;
        }

        @Override
        public int getMaximumStackSize() {
            return maxStackSize;
        }

        @Override
        public boolean isDiggable() {
            return diggable;
        }

        @Override
        public boolean isTransparent() {
            return transparent;
        }

        @Override
        public boolean isFlammable() {
            return flammable;
        }

        @Override
        public int emitsLight() {
            return emitLight;
        }

        @Override
        public int filtersLight() {
            return filterLight;
        }

        @Override
        public float hardness() {
            return hardness;
        }

        @Override
        public boolean isFloodable() {
            return floodable;
        }

        @Override
        public boolean isSolid() {
            return solid;
        }

        @Override
        public int burnChance() {
            return burnChance;
        }

        @Override
        public int burnability() {
            return burnability;
        }

        @Override
        public float resistance() {
            return resistance;
        }

        @Override
        public int getFuelTime() {
            return fuelTime;
        }

        @Override
        public String toString() {
            return "BlockType(" + id + ")";
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
