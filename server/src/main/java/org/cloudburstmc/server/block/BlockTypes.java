package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Builder;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.ToolTypes;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nullable;

public class BlockTypes {

    private static final Reference2ReferenceMap<Identifier, BlockType> BY_ID = new Reference2ReferenceOpenHashMap<>();

    public static final BlockType AIR = IntBlock.builder().id(BlockIds.AIR).maxStackSize(0).floodable(true).friction(0.9f).hardness(-1).replaceable(true).translucency(1).build();
    public static final BlockType STONE = IntBlock.builder().id(BlockIds.STONE).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GRASS = IntBlock.builder().id(BlockIds.GRASS).maxStackSize(64).diggable(true).hardness(0.6f).pushesOutItems(true).resistance(3).solid(true).canBeSilkTouched(true).build();
    public static final BlockType DIRT = IntBlock.builder().id(BlockIds.DIRT).maxStackSize(64).diggable(true).hardness(0.5f).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType COBBLESTONE = IntBlock.builder().id(BlockIds.COBBLESTONE).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType PLANKS = IntBlock.builder().id(BlockIds.PLANKS).maxStackSize(64).diggable(true).hardness(2).fuelTime(300).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType SAPLING = IntBlock.builder().id(BlockIds.SAPLING).maxStackSize(64).diggable(true).floodable(true).fuelTime(100).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType BEDROCK = IntBlock.builder().id(BlockIds.BEDROCK).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).solid(true).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType FLOWING_WATER = IntBlock.builder().id(BlockIds.FLOWING_WATER).maxStackSize(0).filterLight(2).hardness(100).replaceable(true).resistance(500).translucency(1).diggable(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType WATER = IntBlock.builder().id(BlockIds.WATER).maxStackSize(0).filterLight(2).hardness(100).replaceable(true).resistance(500).translucency(1).diggable(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).build(); //TODO: flowing trait
    public static final BlockType FLOWING_LAVA = IntBlock.builder().id(BlockIds.FLOWING_LAVA).maxStackSize(0).emitLight(15).hardness(100).filterLight(14).replaceable(true).resistance(500).translucency(1).diggable(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType LAVA = IntBlock.builder().id(BlockIds.LAVA).maxStackSize(0).emitLight(15).hardness(100).filterLight(14).replaceable(true).resistance(500).translucency(1).diggable(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType SAND = IntBlock.builder().id(BlockIds.SAND).maxStackSize(64).diggable(true).hardness(0.5f).fallable(true).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GRAVEL = IntBlock.builder().id(BlockIds.GRAVEL).maxStackSize(64).diggable(true).hardness(0.6f).fallable(true).pushesOutItems(true).resistance(3).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GOLD_ORE = IntBlock.builder().id(BlockIds.GOLD_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType IRON_ORE = IntBlock.builder().id(BlockIds.IRON_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.STONE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType COAL_ORE = IntBlock.builder().id(BlockIds.COAL_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.STONE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType LOG = IntBlock.builder().id(BlockIds.LOG).maxStackSize(64).diggable(true).hardness(2).burnAbility(5).burnChance(5).flammable(true).pushesOutItems(true).resistance(10).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType LEAVES = IntBlock.builder().id(BlockIds.LEAVES).maxStackSize(64).diggable(true).hardness(0.2f).burnAbility(60).burnChance(30).filterLight(1).flammable(true).pushesOutItems(true).resistance(1).translucency(0.5f).targetTool(ToolTypes.SHEARS).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType SPONGE = IntBlock.builder().id(BlockIds.SPONGE).maxStackSize(64).diggable(true).hardness(0.6f).pushesOutItems(true).resistance(3).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GLASS = IntBlock.builder().id(BlockIds.GLASS).maxStackSize(64).diggable(true).hardness(0.3f).pushesOutItems(true).resistance(1.5f).translucency(1).filterLight(0).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType LAPIS_ORE = IntBlock.builder().id(BlockIds.LAPIS_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.STONE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType LAPIS_BLOCK = IntBlock.builder().id(BlockIds.LAPIS_BLOCK).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType DISPENSER = IntBlock.builder().id(BlockIds.DISPENSER).maxStackSize(64).diggable(true).hardness(3.5f).pushesOutItems(true).resistance(17.5f).solid(true).targetTool(ToolTypes.PICKAXE).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType SANDSTONE = IntBlock.builder().id(BlockIds.SANDSTONE).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType NOTEBLOCK = IntBlock.builder().id(BlockIds.NOTEBLOCK).maxStackSize(64).diggable(true).hardness(0.8f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(4).solid(true).targetTool(ToolTypes.AXE).breakFlowing(true).build();
    public static final BlockType BED = IntBlock.builder().id(BlockIds.BED).maxStackSize(1).diggable(true).hardness(0.2f).floodable(true).resistance(1).translucency(0.1f).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType GOLDEN_RAIL = IntBlock.builder().id(BlockIds.GOLDEN_RAIL).maxStackSize(64).diggable(true).hardness(0.7f).floodable(true).resistance(3.5f).translucency(1).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType DETECTOR_RAIL = IntBlock.builder().id(BlockIds.DETECTOR_RAIL).maxStackSize(64).diggable(true).hardness(0.7f).floodable(true).resistance(3.5f).translucency(1).filterLight(0).powerSource(true).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).comparatorSignal(true).waterlogSource(true).build();
    public static final BlockType STICKY_PISTON = IntBlock.builder().id(BlockIds.STICKY_PISTON).maxStackSize(64).diggable(true).hardness(0.5f).pushesOutItems(true).resistance(2.5f).translucency(0.8f).filterLight(0).pushUpFalling(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType WEB = IntBlock.builder().id(BlockIds.WEB).maxStackSize(64).diggable(true).hardness(4).floodable(true).filterLight(1).resistance(20).translucency(0.8f).targetTool(ToolTypes.SHEARS).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType TALL_GRASS = IntBlock.builder().id(BlockIds.TALL_GRASS).maxStackSize(64).diggable(true).floodable(true).burnAbility(100).burnChance(60).flammable(true).replaceable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType DEADBUSH = IntBlock.builder().id(BlockIds.DEADBUSH).maxStackSize(64).diggable(true).floodable(true).burnAbility(100).burnChance(60).flammable(true).replaceable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType PISTON = IntBlock.builder().id(BlockIds.PISTON).maxStackSize(64).diggable(true).hardness(0.5f).pushesOutItems(true).resistance(2.5f).translucency(0.8f).filterLight(0).pushUpFalling(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType PISTON_ARM_COLLISION = IntBlock.builder().id(BlockIds.PISTON_ARM_COLLISION).maxStackSize(64).diggable(true).hardness(0.5f).resistance(2.5f).translucency(0.8f).filterLight(0).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType WOOL = IntBlock.builder().id(BlockIds.WOOL).maxStackSize(64).diggable(true).hardness(0.8f).burnAbility(60).burnChance(30).flammable(true).pushesOutItems(true).resistance(4).solid(true).translucency(0.8f).targetTool(ToolTypes.SHEARS).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType ELEMENT_0 = IntBlock.builder().id(BlockIds.ELEMENT_0).maxStackSize(64).diggable(true).pushesOutItems(true).solid(true).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: register all elements
    public static final BlockType YELLOW_FLOWER = IntBlock.builder().id(BlockIds.YELLOW_FLOWER).maxStackSize(64).diggable(true).floodable(true).burnAbility(100).burnChance(60).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType RED_FLOWER = IntBlock.builder().id(BlockIds.RED_FLOWER).maxStackSize(64).diggable(true).floodable(true).burnAbility(100).burnChance(60).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType BROWN_MUSHROOM = IntBlock.builder().id(BlockIds.BROWN_MUSHROOM).maxStackSize(64).diggable(true).emitLight(1).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType RED_MUSHROOM = IntBlock.builder().id(BlockIds.RED_MUSHROOM).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType GOLD_BLOCK = IntBlock.builder().id(BlockIds.GOLD_BLOCK).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType IRON_BLOCK = IntBlock.builder().id(BlockIds.IRON_BLOCK).maxStackSize(64).diggable(true).hardness(5).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType DOUBLE_STONE_SLAB = IntBlock.builder().id(BlockIds.DOUBLE_STONE_SLAB).maxStackSize(64).diggable(true).transparent(true).hardness(2).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).breakFlowing(true).build(); //TODO: stone types
    public static final BlockType STONE_SLAB = IntBlock.builder().id(BlockIds.STONE_SLAB).maxStackSize(64).diggable(true).transparent(true).hardness(2).pushesOutItems(true).resistance(30).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build(); //TODO: stone types
    public static final BlockType BRICK_BLOCK = IntBlock.builder().id(BlockIds.BRICK_BLOCK).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType TNT = IntBlock.builder().id(BlockIds.TNT).maxStackSize(64).diggable(true).transparent(true).burnAbility(100).burnChance(15).flammable(true).pushesOutItems(true).solid(true).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType BOOKSHELF = IntBlock.builder().id(BlockIds.BOOKSHELF).maxStackSize(64).diggable(true).hardness(1.5f).fuelTime(300).burnAbility(20).burnChance(30).flammable(true).pushesOutItems(true).resistance(7.5f).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType MOSSY_COBBLESTONE = IntBlock.builder().id(BlockIds.MOSSY_COBBLESTONE).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType OBSIDIAN = IntBlock.builder().id(BlockIds.OBSIDIAN).maxStackSize(64).diggable(true).hardness(35).pushesOutItems(true).resistance(6000).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.DIAMOND).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType TORCH = IntBlock.builder().id(BlockIds.TORCH).maxStackSize(64).diggable(true).emitLight(14).floodable(true).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build(); //TODO: soul type
    public static final BlockType FIRE = IntBlock.builder().id(BlockIds.FIRE).maxStackSize(0).diggable(true).emitLight(15).floodable(true).replaceable(true).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build(); //TODO: soul fire type
    public static final BlockType MOB_SPAWNER = IntBlock.builder().id(BlockIds.MOB_SPAWNER).maxStackSize(64).diggable(true).transparent(true).hardness(5).emitLight(3).pushesOutItems(true).resistance(25).solid(true).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType WOODEN_STAIRS = IntBlock.builder().id(BlockIds.OAK_STAIRS).maxStackSize(64).diggable(true).hardness(2).fuelTime(300).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).resistance(15).translucency(0.8f).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType CHEST = IntBlock.builder().id(BlockIds.CHEST).maxStackSize(64).diggable(true).hardness(2.5f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(12.5f).translucency(0.5f).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType REDSTONE_WIRE = IntBlock.builder().id(BlockIds.REDSTONE_WIRE).maxStackSize(64).diggable(true).translucency(0.8f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType DIAMOND_ORE = IntBlock.builder().id(BlockIds.DIAMOND_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType DIAMOND_BLOCK = IntBlock.builder().id(BlockIds.DIAMOND_BLOCK).maxStackSize(64).diggable(true).hardness(5).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CRAFTING_TABLE = IntBlock.builder().id(BlockIds.CRAFTING_TABLE).maxStackSize(64).diggable(true).hardness(2.5f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(12.5f).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType WHEAT = IntBlock.builder().id(BlockIds.WHEAT).maxStackSize(0).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType FARMLAND = IntBlock.builder().id(BlockIds.FARMLAND).maxStackSize(64).diggable(true).transparent(true).hardness(0.6f).resistance(3).filterLight(0).breakFalling(true).breakFlowing(true).build();
    public static final BlockType FURNACE = IntBlock.builder().id(BlockIds.FURNACE).maxStackSize(64).diggable(true).transparent(true).hardness(3.5f).pushesOutItems(true).resistance(17.5f).solid(true).targetTool(ToolTypes.PICKAXE).comparatorSignal(true).breakFlowing(true).build(); //TODO: lit
    //    public static final BlockType LIT_FURNACE = IntBlock.builder().id(BlockIds.LIT_FURNACE).maxStackSize(64).diggable(true).transparent(true).emitLight(13).hardness(3.5f).pushesOutItems(true).resistance(17.5f).solid(true).targetTool(ToolTypes.PICKAXE).comparatorSignal(true).waterlogFlowing(true).build();
    public static final BlockType STANDING_SIGN = IntBlock.builder().id(BlockIds.OAK_STANDING_SIGN).maxStackSize(16).diggable(true).transparent(true).solid(true).hardness(1f).build();
    public static final BlockType WOODEN_DOOR = IntBlock.builder().id(BlockIds.OAK_DOOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).flammable(true).fuelTime(200).build();
    public static final BlockType LADDER = IntBlock.builder().id(BlockIds.LADDER).maxStackSize(64).diggable(true).hardness(0.4f).fuelTime(300).resistance(2).translucency(1).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType RAIL = IntBlock.builder().id(BlockIds.RAIL).maxStackSize(64).diggable(true).hardness(0.7f).floodable(true).resistance(3.5f).translucency(1).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType STONE_STAIRS = IntBlock.builder().id(BlockIds.STONE_STAIRS).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build(); //TODO: stone types
    public static final BlockType WALL_SIGN = IntBlock.builder().id(BlockIds.OAK_WALL_SIGN).maxStackSize(16).diggable(true).transparent(true).solid(true).hardness(1f).build();
    public static final BlockType LEVER = IntBlock.builder().id(BlockIds.LEVER).maxStackSize(64).diggable(true).hardness(0.5f).floodable(true).resistance(2.5f).translucency(1).filterLight(0).powerSource(true).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType STONE_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.STONE_PRESSURE_PLATE).maxStackSize(64).diggable(true).hardness(0.5f).resistance(2.5f).translucency(0.2f).targetTool(ToolTypes.PICKAXE).filterLight(0).powerSource(true).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build(); //TODO: blackstone type
    public static final BlockType IRON_DOOR = IntBlock.builder().id(BlockIds.IRON_DOOR).maxStackSize(64).diggable(true).hardness(5).resistance(25).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType WOODEN_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.OAK_PRESSURE_PLATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).fuelTime(300).build();
    public static final BlockType REDSTONE_ORE = IntBlock.builder().id(BlockIds.REDSTONE_ORE).maxStackSize(64).diggable(true).transparent(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: lit type
    public static final BlockType REDSTONE_TORCH = IntBlock.builder().id(BlockIds.REDSTONE_TORCH).maxStackSize(64).diggable(true).emitLight(7).floodable(true).translucency(1).filterLight(0).powerSource(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType STONE_BUTTON = IntBlock.builder().id(BlockIds.STONE_BUTTON).maxStackSize(64).diggable(true).transparent(true).hardness(0.5f).floodable(true).resistance(2.5f).targetTool(ToolTypes.PICKAXE).filterLight(0).powerSource(true).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build(); //TODO: blackstone
    public static final BlockType SNOW_LAYER = IntBlock.builder().id(BlockIds.SNOW_LAYER).maxStackSize(64).diggable(true).hardness(0.1f).floodable(true).fallable(true).replaceable(true).resistance(0.5f).translucency(0.89f).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType ICE = IntBlock.builder().id(BlockIds.ICE).maxStackSize(64).diggable(true).transparent(true).hardness(0.5f).filterLight(3).friction(0.98f).pushesOutItems(true).resistance(2.5f).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType SNOW = IntBlock.builder().id(BlockIds.SNOW).maxStackSize(64).diggable(true).hardness(0.2f).pushesOutItems(true).resistance(1).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CACTUS = IntBlock.builder().id(BlockIds.CACTUS).maxStackSize(64).diggable(true).hardness(0.4f).pushesOutItems(true).resistance(2).translucency(0.5f).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType CLAY = IntBlock.builder().id(BlockIds.CLAY).maxStackSize(64).diggable(true).hardness(0.6f).pushesOutItems(true).resistance(3).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType REEDS = IntBlock.builder().id(BlockIds.REEDS).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType JUKEBOX = IntBlock.builder().id(BlockIds.JUKEBOX).maxStackSize(64).diggable(true).hardness(0.8f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(4).solid(true).targetTool(ToolTypes.AXE).powerSource(true).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType WOODEN_FENCE = IntBlock.builder().id(BlockIds.FENCE).maxStackSize(64).diggable(true).hardness(2).fuelTime(300).burnAbility(20).burnChance(5).flammable(true).resistance(15).translucency(0.8f).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType PUMPKIN = IntBlock.builder().id(BlockIds.PUMPKIN).maxStackSize(64).diggable(true).transparent(true).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType NETHERRACK = IntBlock.builder().id(BlockIds.NETHERRACK).maxStackSize(64).diggable(true).hardness(0.4f).pushesOutItems(true).resistance(2).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType SOUL_SAND = IntBlock.builder().id(BlockIds.SOUL_SAND).maxStackSize(64).diggable(true).hardness(0.5f).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GLOWSTONE = IntBlock.builder().id(BlockIds.GLOWSTONE).maxStackSize(64).diggable(true).emitLight(15).hardness(0.3f).pushesOutItems(true).resistance(1.5f).solid(true).translucency(1).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType PORTAL = IntBlock.builder().id(BlockIds.PORTAL).maxStackSize(0).emitLight(11).hardness(-1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType LIT_PUMPKIN = IntBlock.builder().id(BlockIds.LIT_PUMPKIN).maxStackSize(64).diggable(true).transparent(true).emitLight(15).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CAKE = IntBlock.builder().id(BlockIds.CAKE).maxStackSize(1).diggable(true).hardness(0.5f).pushesOutItems(true).resistance(2.5f).translucency(0.8f).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType REPEATER = IntBlock.builder().id(BlockIds.UNPOWERED_REPEATER).maxStackSize(64).diggable(true).translucency(1).filterLight(0).powerSource(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType INVISIBLE_BEDROCK = IntBlock.builder().id(BlockIds.INVISIBLE_BEDROCK).maxStackSize(64).hardness(-1).resistance(1.8E7f).resistance(1.8E7f).translucency(0.8f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).filterLight(0).breakFalling(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType WOODEN_TRAPDOOR = IntBlock.builder().id(BlockIds.OAK_TRAPDOOR).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(3f).fuelTime(300).build();
    public static final BlockType MONSTER_EGG = IntBlock.builder().id(BlockIds.MONSTER_EGG).maxStackSize(64).diggable(true).hardness(0.75f).pushesOutItems(true).resistance(3.75f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType STONEBRICK = IntBlock.builder().id(BlockIds.STONEBRICK).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType BROWN_MUSHROOM_BLOCK = IntBlock.builder().id(BlockIds.BROWN_MUSHROOM_BLOCK).maxStackSize(64).diggable(true).hardness(0.2f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(1).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType RED_MUSHROOM_BLOCK = IntBlock.builder().id(BlockIds.RED_MUSHROOM_BLOCK).maxStackSize(64).diggable(true).hardness(0.2f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(1).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType IRON_BARS = IntBlock.builder().id(BlockIds.IRON_BARS).maxStackSize(64).diggable(true).hardness(5).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType GLASS_PANE = IntBlock.builder().id(BlockIds.GLASS_PANE).maxStackSize(64).diggable(true).hardness(0.3f).resistance(1.5f).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType MELON_BLOCK = IntBlock.builder().id(BlockIds.MELON_BLOCK).maxStackSize(64).diggable(true).transparent(true).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType PUMPKIN_STEM = IntBlock.builder().id(BlockIds.PUMPKIN_STEM).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType MELON_STEM = IntBlock.builder().id(BlockIds.MELON_STEM).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType VINE = IntBlock.builder().id(BlockIds.VINE).maxStackSize(64).diggable(true).hardness(0.2f).floodable(true).burnAbility(100).burnChance(15).flammable(true).replaceable(true).resistance(1).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType WOODEN_FENCE_GATE = IntBlock.builder().id(BlockIds.OAK_FENCE_GATE).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(2f).fuelTime(300).build();
    public static final BlockType BRICK_STAIRS = IntBlock.builder().id(BlockIds.BRICK_STAIRS).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType STONE_BRICK_STAIRS = IntBlock.builder().id(BlockIds.STONE_BRICK_STAIRS).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType MYCELIUM = IntBlock.builder().id(BlockIds.MYCELIUM).maxStackSize(64).diggable(true).hardness(0.6f).pushesOutItems(true).resistance(3).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType WATERLILY = IntBlock.builder().id(BlockIds.WATERLILY).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType NETHER_BRICK = IntBlock.builder().id(BlockIds.NETHER_BRICK).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: nether brick type red/cracked/chiseled
    public static final BlockType NETHER_BRICK_FENCE = IntBlock.builder().id(BlockIds.NETHER_BRICK_FENCE).maxStackSize(64).diggable(true).hardness(2).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType NETHER_BRICK_STAIRS = IntBlock.builder().id(BlockIds.NETHER_BRICK_STAIRS).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType NETHER_WART = IntBlock.builder().id(BlockIds.NETHER_WART).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType ENCHANTING_TABLE = IntBlock.builder().id(BlockIds.ENCHANTING_TABLE).maxStackSize(64).diggable(true).transparent(true).hardness(5).emitLight(12).pushesOutItems(true).resistance(6000).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BREWING_STAND = IntBlock.builder().id(BlockIds.BREWING_STAND).maxStackSize(64).diggable(true).hardness(0.5f).filterLight(3).pushesOutItems(true).resistance(2.5f).translucency(0.8f).targetTool(ToolTypes.PICKAXE).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType CAULDRON = IntBlock.builder().id(BlockIds.CAULDRON).maxStackSize(64).diggable(true).hardness(2).filterLight(3).resistance(10).translucency(0.8f).targetTool(ToolTypes.PICKAXE).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).build(); //TODO: lava
    public static final BlockType END_PORTAL = IntBlock.builder().id(BlockIds.END_PORTAL).maxStackSize(64).transparent(true).emitLight(15).hardness(-1).pushesOutItems(true).resistance(1.8E7f).resistance(1.8E7f).resistance(1.8E7f).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType END_PORTAL_FRAME = IntBlock.builder().id(BlockIds.END_PORTAL_FRAME).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).resistance(1.8E7f).translucency(0.8f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType END_STONE = IntBlock.builder().id(BlockIds.END_STONE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(45).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType DRAGON_EGG = IntBlock.builder().id(BlockIds.DRAGON_EGG).maxStackSize(64).transparent(true).emitLight(1).hardness(3).fallable(true).pushesOutItems(true).resistance(15).targetTool(ToolTypes.PICKAXE).filterLight(0).diggable(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType REDSTONE_LAMP = IntBlock.builder().id(BlockIds.REDSTONE_LAMP).maxStackSize(64).diggable(true).hardness(0.3f).pushesOutItems(true).resistance(1.5f).solid(true).translucency(1).canBeSilkTouched(true).breakFlowing(true).build();
    //    public static final BlockType LIT_REDSTONE_LAMP = IntBlock.builder().id(BlockIds.LIT_REDSTONE_LAMP).maxStackSize(64).diggable(true).emitLight(15).hardness(0.3f).pushesOutItems(true).resistance(1.5f).solid(true).translucency(1).canBeSilkTouched(true).waterlogFlowing(true).build();
    public static final BlockType DROPPER = IntBlock.builder().id(BlockIds.DROPPER).maxStackSize(64).diggable(true).hardness(3.5f).pushesOutItems(true).resistance(17.5f).solid(true).targetTool(ToolTypes.PICKAXE).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType ACTIVATOR_RAIL = IntBlock.builder().id(BlockIds.ACTIVATOR_RAIL).maxStackSize(64).diggable(true).hardness(0.5f).floodable(true).resistance(2.5f).translucency(1).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType COCOA = IntBlock.builder().id(BlockIds.COCOA).maxStackSize(64).diggable(true).hardness(0.2f).resistance(15).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType SANDSTONE_STAIRS = IntBlock.builder().id(BlockIds.SANDSTONE_STAIRS).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType EMERALD_ORE = IntBlock.builder().id(BlockIds.EMERALD_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType ENDER_CHEST = IntBlock.builder().id(BlockIds.ENDER_CHEST).maxStackSize(64).diggable(true).hardness(22.5f).emitLight(7).pushesOutItems(true).resistance(3000).translucency(0.5f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType TRIPWIRE_HOOK = IntBlock.builder().id(BlockIds.TRIPWIRE_HOOK).maxStackSize(64).diggable(true).floodable(true).translucency(1).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType TRIPWIRE = IntBlock.builder().id(BlockIds.TRIPWIRE).maxStackSize(64).diggable(true).floodable(true).translucency(1).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType EMERALD_BLOCK = IntBlock.builder().id(BlockIds.EMERALD_BLOCK).maxStackSize(64).diggable(true).hardness(5).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType COMMAND_BLOCK = IntBlock.builder().id(BlockIds.COMMAND_BLOCK).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).solid(true).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType BEACON = IntBlock.builder().id(BlockIds.BEACON).maxStackSize(64).diggable(true).emitLight(14).hardness(3).filterLight(14).pushesOutItems(true).resistance(15).solid(true).translucency(1).blockSolid(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType COBBLESTONE_WALL = IntBlock.builder().id(BlockIds.COBBLESTONE_WALL).maxStackSize(64).diggable(true).hardness(2).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build(); //TODO: blackstone
    public static final BlockType FLOWER_POT = IntBlock.builder().id(BlockIds.FLOWER_POT).maxStackSize(64).diggable(true).floodable(true).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CARROTS = IntBlock.builder().id(BlockIds.CARROTS).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType POTATOES = IntBlock.builder().id(BlockIds.POTATOES).maxStackSize(64).diggable(true).floodable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType WOODEN_BUTTON = IntBlock.builder().id(BlockIds.OAK_BUTTON).maxStackSize(64).diggable(true).transparent(true).solid(true).hardness(0.5f).floodable(true).fuelTime(100).build();
    public static final BlockType SKULL = IntBlock.builder().id(BlockIds.SKULL).maxStackSize(64).diggable(true).hardness(1).floodable(true).resistance(5).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType ANVIL = IntBlock.builder().id(BlockIds.ANVIL).maxStackSize(64).diggable(true).hardness(5).fallable(true).filterLight(3).pushesOutItems(true).resistance(6000).translucency(0.8f).targetTool(ToolTypes.PICKAXE).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType TRAPPED_CHEST = IntBlock.builder().id(BlockIds.TRAPPED_CHEST).maxStackSize(64).diggable(true).hardness(2.5f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(12.5f).translucency(0.5f).targetTool(ToolTypes.AXE).filterLight(0).powerSource(true).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType LIGHT_WEIGHTED_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.LIGHT_WEIGHTED_PRESSURE_PLATE).maxStackSize(64).diggable(true).hardness(0.5f).resistance(2.5f).translucency(0.2f).targetTool(ToolTypes.PICKAXE).filterLight(0).powerSource(true).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType HEAVY_WEIGHTED_PRESSURE_PLATE = IntBlock.builder().id(BlockIds.HEAVY_WEIGHTED_PRESSURE_PLATE).maxStackSize(64).diggable(true).hardness(0.5f).resistance(2.5f).translucency(0.2f).targetTool(ToolTypes.PICKAXE).filterLight(0).powerSource(true).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType COMPARATOR = IntBlock.builder().id(BlockIds.UNPOWERED_COMPARATOR).maxStackSize(64).diggable(true).translucency(1).filterLight(0).powerSource(true).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType DAYLIGHT_DETECTOR = IntBlock.builder().id(BlockIds.DAYLIGHT_DETECTOR).maxStackSize(64).diggable(true).transparent(true).hardness(0.2f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(1).targetTool(ToolTypes.AXE).filterLight(0).powerSource(true).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType REDSTONE_BLOCK = IntBlock.builder().id(BlockIds.REDSTONE_BLOCK).maxStackSize(64).diggable(true).transparent(true).hardness(5).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).powerSource(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType QUARTZ_ORE = IntBlock.builder().id(BlockIds.QUARTZ_ORE).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.STONE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType HOPPER = IntBlock.builder().id(BlockIds.HOPPER).maxStackSize(64).diggable(true).hardness(3).filterLight(3).resistance(24).translucency(0.8f).targetTool(ToolTypes.PICKAXE).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType QUARTZ_BLOCK = IntBlock.builder().id(BlockIds.QUARTZ_BLOCK).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: quartz bricks
    public static final BlockType QUARTZ_STAIRS = IntBlock.builder().id(BlockIds.QUARTZ_STAIRS).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType DOUBLE_WOODEN_SLAB = IntBlock.builder().id(BlockIds.DOUBLE_WOODEN_SLAB).maxStackSize(64).diggable(true).transparent(true).hardness(2).fuelTime(300).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.AXE).breakFlowing(true).build(); //TODO: wood types or add half trait?
    public static final BlockType WOODEN_SLAB = IntBlock.builder().id(BlockIds.WOODEN_SLAB).maxStackSize(64).diggable(true).transparent(true).hardness(2).fuelTime(300).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).resistance(15).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType STAINED_HARDENED_CLAY = IntBlock.builder().id(BlockIds.STAINED_HARDENED_CLAY).maxStackSize(64).diggable(true).hardness(1.25f).pushesOutItems(true).resistance(21).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType STAINED_GLASS_PANE = IntBlock.builder().id(BlockIds.STAINED_GLASS_PANE).maxStackSize(64).diggable(true).hardness(0.3f).resistance(1.5f).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType SLIME = IntBlock.builder().id(BlockIds.SLIME).maxStackSize(64).diggable(true).friction(0.8f).pushesOutItems(true).solid(true).translucency(0.8f).filterLight(0).canBeSilkTouched(true).breakFlowing(true).build();
    //    public static final BlockType GLOW_STICK = IntBlock.builder().id(BlockIds.GLOW_STICK).maxStackSize(64).diggable(true).transparent(true).solid(true).emitLight(14).hardness(0f).floodable(true).build();
    public static final BlockType IRON_TRAPDOOR = IntBlock.builder().id(BlockIds.IRON_TRAPDOOR).maxStackSize(64).diggable(true).hardness(5).resistance(25).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType PRISMARINE = IntBlock.builder().id(BlockIds.PRISMARINE).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType SEA_LANTERN = IntBlock.builder().id(BlockIds.SEA_LANTERN).maxStackSize(64).diggable(true).emitLight(15).hardness(0.3f).pushesOutItems(true).resistance(1.5f).solid(true).translucency(1).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType HAY_BLOCK = IntBlock.builder().id(BlockIds.HAY_BLOCK).maxStackSize(64).diggable(true).hardness(0.5f).burnAbility(20).burnChance(60).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CARPET = IntBlock.builder().id(BlockIds.CARPET).maxStackSize(64).diggable(true).hardness(0.1f).floodable(true).burnAbility(20).burnChance(30).flammable(true).resistance(0.5f).translucency(0.89f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType HARDENED_CLAY = IntBlock.builder().id(BlockIds.HARDENED_CLAY).maxStackSize(64).diggable(true).hardness(1.25f).pushesOutItems(true).resistance(21).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType COAL_BLOCK = IntBlock.builder().id(BlockIds.COAL_BLOCK).maxStackSize(64).diggable(true).hardness(5).fuelTime(16000).burnAbility(5).burnChance(5).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType PACKED_ICE = IntBlock.builder().id(BlockIds.PACKED_ICE).maxStackSize(64).diggable(true).hardness(0.5f).friction(0.98f).pushesOutItems(true).resistance(2.5f).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType DOUBLE_PLANT = IntBlock.builder().id(BlockIds.DOUBLE_PLANT).maxStackSize(64).diggable(true).floodable(true).burnAbility(100).burnChance(60).flammable(true).replaceable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType STANDING_BANNER = IntBlock.builder().id(BlockIds.STANDING_BANNER).maxStackSize(16).diggable(true).hardness(1).flammable(true).resistance(5).translucency(0.8f).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType WALL_BANNER = IntBlock.builder().id(BlockIds.WALL_BANNER).maxStackSize(16).diggable(true).hardness(1).flammable(true).resistance(5).translucency(0.8f).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType DAYLIGHT_DETECTOR_INVERTED = IntBlock.builder().id(BlockIds.DAYLIGHT_DETECTOR_INVERTED).maxStackSize(64).diggable(true).transparent(true).hardness(0.2f).fuelTime(300).flammable(true).pushesOutItems(true).resistance(1).targetTool(ToolTypes.AXE).filterLight(0).powerSource(true).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType RED_SANDSTONE = IntBlock.builder().id(BlockIds.RED_SANDSTONE).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType RED_SANDSTONE_STAIRS = IntBlock.builder().id(BlockIds.RED_SANDSTONE_STAIRS).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType REPEATING_COMMAND_BLOCK = IntBlock.builder().id(BlockIds.REPEATING_COMMAND_BLOCK).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).solid(true).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType CHAIN_COMMAND_BLOCK = IntBlock.builder().id(BlockIds.CHAIN_COMMAND_BLOCK).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).solid(true).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType HARD_GLASS_PANE = IntBlock.builder().id(BlockIds.HARD_GLASS_PANE).maxStackSize(64).diggable(true).hardness(10).resistance(50).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType HARD_STAINED_GLASS_PANE = IntBlock.builder().id(BlockIds.HARD_STAINED_GLASS_PANE).maxStackSize(64).diggable(true).hardness(10).resistance(50).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType CHEMICAL_HEAT = IntBlock.builder().id(BlockIds.CHEMICAL_HEAT).maxStackSize(64).diggable(true).hardness(2.5f).pushesOutItems(true).resistance(12.5f).solid(true).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GRASS_PATH = IntBlock.builder().id(BlockIds.GRASS_PATH).maxStackSize(64).diggable(true).transparent(true).hardness(0.65f).resistance(3.25f).filterLight(0).breakFalling(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType FRAME = IntBlock.builder().id(BlockIds.FRAME).maxStackSize(64).diggable(true).hardness(0.25f).floodable(true).resistance(1.25f).translucency(1).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType CHORUS_FLOWER = IntBlock.builder().id(BlockIds.CHORUS_FLOWER).maxStackSize(64).diggable(true).transparent(true).hardness(0.4f).flammable(true).pushesOutItems(true).resistance(2).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).build();
    public static final BlockType PURPUR_BLOCK = IntBlock.builder().id(BlockIds.PURPUR_BLOCK).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType COLORED_TORCH_RG = IntBlock.builder().id(BlockIds.COLORED_TORCH_RG).maxStackSize(64).diggable(true).emitLight(14).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType PURPUR_STAIRS = IntBlock.builder().id(BlockIds.PURPUR_STAIRS).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType COLORED_TORCH_BP = IntBlock.builder().id(BlockIds.COLORED_TORCH_BP).maxStackSize(64).diggable(true).emitLight(14).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType UNDYED_SHULKER_BOX = IntBlock.builder().id(BlockIds.UNDYED_SHULKER_BOX).maxStackSize(1).diggable(true).hardness(2.5f).pushesOutItems(true).resistance(12.5f).translucency(0.5f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType END_BRICKS = IntBlock.builder().id(BlockIds.END_BRICKS).maxStackSize(64).diggable(true).hardness(0.8f).pushesOutItems(true).resistance(4).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType FROSTED_ICE = IntBlock.builder().id(BlockIds.FROSTED_ICE).maxStackSize(64).diggable(true).transparent(true).filterLight(3).hardness(0.5f).friction(0.98f).resistance(2.5f).blockSolid(false).breakFlowing(true).build();
    public static final BlockType END_ROD = IntBlock.builder().id(BlockIds.END_ROD).maxStackSize(64).diggable(true).emitLight(14).floodable(true).translucency(1).filterLight(0).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType END_GATEWAY = IntBlock.builder().id(BlockIds.END_GATEWAY).maxStackSize(64).emitLight(15).hardness(-1).pushesOutItems(true).resistance(1.8E7f).resistance(1.8E7f).resistance(1.8E7f).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType ALLOW = IntBlock.builder().id(BlockIds.ALLOW).maxStackSize(64).hardness(0.2f).pushesOutItems(true).resistance(18000).solid(true).diggable(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType DENY = IntBlock.builder().id(BlockIds.DENY).maxStackSize(64).hardness(0.2f).pushesOutItems(true).resistance(18000).solid(true).diggable(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType BORDER_BLOCK = IntBlock.builder().id(BlockIds.BORDER_BLOCK).maxStackSize(64).hardness(0.2f).resistance(18000).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).diggable(true).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType MAGMA = IntBlock.builder().id(BlockIds.MAGMA).maxStackSize(64).diggable(true).emitLight(3).hardness(0.5f).pushesOutItems(true).resistance(7.5f).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType NETHER_WART_BLOCK = IntBlock.builder().id(BlockIds.NETHER_WART_BLOCK).maxStackSize(64).diggable(true).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: warped type
    public static final BlockType RED_NETHER_BRICK = IntBlock.builder().id(BlockIds.RED_NETHER_BRICK).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(30).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType BONE_BLOCK = IntBlock.builder().id(BlockIds.BONE_BLOCK).maxStackSize(64).diggable(true).hardness(2).pushesOutItems(true).resistance(10).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType SHULKER_BOX = IntBlock.builder().id(BlockIds.SHULKER_BOX).maxStackSize(1).diggable(true).hardness(2.5f).pushesOutItems(true).resistance(12.5f).translucency(0.5f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType GLAZED_TERRACOTTA = IntBlock.builder().id(BlockIds.WHITE_GLAZED_TERRACOTTA).maxStackSize(64).diggable(true).hardness(1.4f).pushesOutItems(true).resistance(7).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: colors
    public static final BlockType CONCRETE = IntBlock.builder().id(BlockIds.CONCRETE).maxStackSize(64).diggable(true).hardness(1.8f).pushesOutItems(true).resistance(9).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CONCRETE_POWDER = IntBlock.builder().id(BlockIds.CONCRETE_POWDER).maxStackSize(64).diggable(true).hardness(0.5f).fallable(true).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CHEMISTRY_TABLE = IntBlock.builder().id(BlockIds.CHEMISTRY_TABLE).maxStackSize(64).diggable(true).hardness(2.5f).resistance(12.5f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockWater(false).breakFlowing(true).build();
    public static final BlockType UNDERWATER_TORCH = IntBlock.builder().id(BlockIds.UNDERWATER_TORCH).maxStackSize(64).diggable(true).emitLight(14).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CHORUS_PLANT = IntBlock.builder().id(BlockIds.CHORUS_PLANT).maxStackSize(64).diggable(true).transparent(true).hardness(0.4f).flammable(true).resistance(2).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).build();
    public static final BlockType STAINED_GLASS = IntBlock.builder().id(BlockIds.STAINED_GLASS).maxStackSize(64).diggable(true).hardness(0.3f).pushesOutItems(true).resistance(1.5f).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType CAMERA = IntBlock.builder().id(BlockIds.CAMERA).maxStackSize(64).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).diggable(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType PODZOL = IntBlock.builder().id(BlockIds.PODZOL).maxStackSize(64).diggable(true).hardness(0.5f).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType BEETROOT = IntBlock.builder().id(BlockIds.BEETROOT).maxStackSize(64).diggable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType STONECUTTER = IntBlock.builder().id(BlockIds.STONECUTTER).maxStackSize(64).diggable(true).hardness(3.5f).pushesOutItems(true).resistance(17.5f).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType GLOWING_OBSIDIAN = IntBlock.builder().id(BlockIds.GLOWING_OBSIDIAN).maxStackSize(64).diggable(true).emitLight(13).hardness(10).pushesOutItems(true).resistance(6000).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.DIAMOND).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType NETHER_REACTOR = IntBlock.builder().id(BlockIds.NETHER_REACTOR).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).targetTool(ToolTypes.PICKAXE).breakFlowing(true).build();
    public static final BlockType INFO_UPDATE = IntBlock.builder().id(BlockIds.INFO_UPDATE).maxStackSize(64).diggable(true).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType INFO_UPDATE2 = IntBlock.builder().id(BlockIds.INFO_UPDATE2).maxStackSize(64).diggable(true).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType MOVING_BLOCK = IntBlock.builder().id(BlockIds.MOVING_BLOCK).maxStackSize(64).transparent(true).hardness(-1).resistance(1.8E7f).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).pushUpFalling(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType OBSERVER = IntBlock.builder().id(BlockIds.OBSERVER).maxStackSize(64).diggable(true).hardness(3).pushesOutItems(true).resistance(15).solid(true).translucency(0.8f).targetTool(ToolTypes.PICKAXE).powerSource(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType STRUCTURE_BLOCK = IntBlock.builder().id(BlockIds.STRUCTURE_BLOCK).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).solid(true).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType HARD_GLASS = IntBlock.builder().id(BlockIds.HARD_GLASS).maxStackSize(64).diggable(true).hardness(10).pushesOutItems(true).resistance(50).translucency(1).filterLight(0).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType HARD_STAINED_GLASS = IntBlock.builder().id(BlockIds.HARD_STAINED_GLASS).maxStackSize(64).diggable(true).hardness(10).pushesOutItems(true).resistance(50).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).breakFlowing(true).build();
    public static final BlockType RESERVED6 = IntBlock.builder().id(BlockIds.RESERVED6).maxStackSize(64).diggable(true).pushesOutItems(true).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType PRISMARINE_STAIRS = IntBlock.builder().id(BlockIds.PRISMARINE_STAIRS).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType DARK_PRISMARINE_STAIRS = IntBlock.builder().id(BlockIds.DARK_PRISMARINE_STAIRS).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType PRISMARINE_BRICKS_STAIRS = IntBlock.builder().id(BlockIds.PRISMARINE_BRICKS_STAIRS).maxStackSize(64).diggable(true).hardness(1.5f).pushesOutItems(true).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BLUE_ICE = IntBlock.builder().id(BlockIds.BLUE_ICE).maxStackSize(64).diggable(true).transparent(true).hardness(2.8f).emitLight(4).friction(0.989f).pushesOutItems(true).resistance(14).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();

    public static final BlockType SEAGRASS = IntBlock.builder().id(BlockIds.SEAGRASS).maxStackSize(64).diggable(true).flammable(true).replaceable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CORAL = IntBlock.builder().id(BlockIds.CORAL).maxStackSize(64).diggable(true).translucency(0.8f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CORAL_BLOCK = IntBlock.builder().id(BlockIds.CORAL_BLOCK).maxStackSize(64).diggable(true).hardness(7).pushesOutItems(true).resistance(4.5f).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CORAL_FAN = IntBlock.builder().id(BlockIds.CORAL_FAN).maxStackSize(64).diggable(true).resistance(4.5f).translucency(0.8f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CORAL_FAN_DEAD = IntBlock.builder().id(BlockIds.CORAL_FAN_DEAD).maxStackSize(64).diggable(true).resistance(4.5f).translucency(0.8f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CORAL_FAN_HANG = IntBlock.builder().id(BlockIds.CORAL_FAN_HANG).maxStackSize(64).diggable(true).resistance(4.5f).translucency(0.8f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType KELP = IntBlock.builder().id(BlockIds.KELP).maxStackSize(64).diggable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType DRIED_KELP_BLOCK = IntBlock.builder().id(BlockIds.DRIED_KELP_BLOCK).maxStackSize(64).diggable(true).hardness(0.5f).fuelTime(4000).burnAbility(5).burnChance(30).pushesOutItems(true).resistance(2.5f).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CARVED_PUMPKIN = IntBlock.builder().id(BlockIds.CARVED_PUMPKIN).maxStackSize(64).diggable(true).transparent(true).hardness(1).pushesOutItems(true).resistance(5).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType SEA_PICKLE = IntBlock.builder().id(BlockIds.SEA_PICKLE).maxStackSize(64).diggable(true).burnAbility(100).burnChance(15).emitLight(4).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).waterlogSource(true).build();
    public static final BlockType CONDUIT = IntBlock.builder().id(BlockIds.CONDUIT).maxStackSize(64).diggable(true).hardness(3).emitLight(15).resistance(15).translucency(1).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();

    public static final BlockType TURTLE_EGG = IntBlock.builder().id(BlockIds.TURTLE_EGG).maxStackSize(64).diggable(true).transparent(true).hardness(0.4f).resistance(2).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BUBBLE_COLUMN = IntBlock.builder().id(BlockIds.BUBBLE_COLUMN).maxStackSize(64).hardness(-1).replaceable(true).translucency(0.9f).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BARRIER = IntBlock.builder().id(BlockIds.BARRIER).maxStackSize(64).hardness(-1).pushesOutItems(true).resistance(1.8E7f).solid(true).translucency(1).resistance(1.8E7f).resistance(1.8E7f).filterLight(0).canBeSilkTouched(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BAMBOO = IntBlock.builder().id(BlockIds.BAMBOO).maxStackSize(64).diggable(true).hardness(2).burnAbility(20).burnChance(5).resistance(10).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType BAMBOO_SAPLING = IntBlock.builder().id(BlockIds.BAMBOO_SAPLING).maxStackSize(64).diggable(true).burnAbility(20).burnChance(5).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build();
    public static final BlockType SCAFFOLDING = IntBlock.builder().id(BlockIds.SCAFFOLDING).maxStackSize(64).diggable(true).fuelTime(1200).hardness(0.6f).burnAbility(20).burnChance(60).fallable(true).flammable(true).resistance(4.5f).translucency(1).filterLight(0).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType LECTERN = IntBlock.builder().id(BlockIds.LECTERN).maxStackSize(64).diggable(true).hardness(2).resistance(10).fuelTime(300).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).translucency(0.8f).targetTool(ToolTypes.AXE).filterLight(0).powerSource(true).breakFalling(true).blockSolid(false).blockMotion(false).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType GRINDSTONE = IntBlock.builder().id(BlockIds.GRINDSTONE).maxStackSize(64).diggable(true).hardness(2).resistance(30).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BLAST_FURNACE = IntBlock.builder().id(BlockIds.BLAST_FURNACE).maxStackSize(64).diggable(true).hardness(3.5f).resistance(17.5f).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType STONECUTTER_BLOCK = IntBlock.builder().id(BlockIds.STONECUTTER_BLOCK).maxStackSize(64).diggable(true).transparent(true).hardness(3.5f).resistance(17.5f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType SMOKER = IntBlock.builder().id(BlockIds.SMOKER).maxStackSize(64).diggable(true).hardness(3.5f).resistance(17.5f).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType CARTOGRAPHY_TABLE = IntBlock.builder().id(BlockIds.CARTOGRAPHY_TABLE).maxStackSize(64).diggable(true).hardness(2.5f).resistance(12.5f).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType FLETCHING_TABLE = IntBlock.builder().id(BlockIds.FLETCHING_TABLE).maxStackSize(64).diggable(true).hardness(2.5f).resistance(12.5f).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType SMITHING_TABLE = IntBlock.builder().id(BlockIds.SMITHING_TABLE).maxStackSize(64).diggable(true).hardness(2.5f).resistance(12.5f).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType BARREL = IntBlock.builder().id(BlockIds.BARREL).maxStackSize(64).diggable(true).hardness(2.5f).resistance(12.5f).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).powerSource(true).canBeSilkTouched(true).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType LOOM = IntBlock.builder().id(BlockIds.LOOM).maxStackSize(64).diggable(true).hardness(2.5f).resistance(12.5f).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType BELL = IntBlock.builder().id(BlockIds.BELL).maxStackSize(64).diggable(true).transparent(true).hardness(1).resistance(15).burnAbility(20).burnChance(5).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType SWEET_BERRY_BUSH = IntBlock.builder().id(BlockIds.SWEET_BERRY_BUSH).maxStackSize(64).diggable(true).hardness(0.2f).burnAbility(100).burnChance(60).resistance(1).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build();
    public static final BlockType LANTERN = IntBlock.builder().id(BlockIds.LANTERN).maxStackSize(64).diggable(true).hardness(5).resistance(25).emitLight(15).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).build(); //TODO: lantern type
    public static final BlockType CAMPFIRE = IntBlock.builder().id(BlockIds.CAMPFIRE).diggable(true).hardness(5).resistance(25).burnAbility(20).burnChance(5).translucency(1).filterLight(0).breakFalling(true).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build(); //TODO: campfire type
    public static final BlockType JIGSAW = IntBlock.builder().id(BlockIds.JIGSAW).maxStackSize(64).resistance(1.8E7f).hardness(-1).pushesOutItems(true).solid(true).resistance(1.8E7f).targetTool(ToolTypes.PICKAXE).resistance(1.8E7f).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType WOOD = IntBlock.builder().id(BlockIds.WOOD).maxStackSize(64).diggable(true).hardness(2).resistance(10).burnAbility(5).burnChance(5).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: properties per trait? //hyphae types
    public static final BlockType COMPOSTER = IntBlock.builder().id(BlockIds.COMPOSTER).maxStackSize(64).diggable(true).hardness(2).resistance(10).burnAbility(20).burnChance(5).flammable(true).translucency(0.8f).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).canBeSilkTouched(true).comparatorSignal(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType LIGHT_BLOCK = IntBlock.builder().id(BlockIds.LIGHT_BLOCK).maxStackSize(64).friction(0.9f).hardness(-1).replaceable(true).translucency(1).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType BEE_NEST = IntBlock.builder().id(BlockIds.BEE_NEST).maxStackSize(64).diggable(true).hardness(0.3f).resistance(13.5f).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType BEEHIVE = IntBlock.builder().id(BlockIds.BEEHIVE).maxStackSize(64).diggable(true).hardness(0.6f).resistance(27).burnAbility(20).burnChance(5).flammable(true).pushesOutItems(true).solid(true).targetTool(ToolTypes.AXE).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType HONEY_BLOCK = IntBlock.builder().id(BlockIds.HONEY_BLOCK).maxStackSize(64).diggable(true).friction(0.8f).pushesOutItems(true).solid(true).translucency(0.8f).filterLight(0).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType HONEYCOMB_BLOCK = IntBlock.builder().id(BlockIds.HONEYCOMB_BLOCK).maxStackSize(64).diggable(true).hardness(0.6f).resistance(3).pushesOutItems(true).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType LODESTONE = IntBlock.builder().id(BlockIds.LODESTONE).maxStackSize(64).diggable(true).hardness(2).resistance(10).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).breakFlowing(true).build();
    public static final BlockType CRIMSON_ROOTS = IntBlock.builder().id(BlockIds.CRIMSON_ROOTS).maxStackSize(64).diggable(true).burnAbility(100).burnChance(60).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build(); //TODO: roots type
    public static final BlockType CRIMSON_FUNGUS = IntBlock.builder().id(BlockIds.CRIMSON_FUNGUS).maxStackSize(64).diggable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).build(); //TODO: fungus type
    public static final BlockType SHROOMLIGHT = IntBlock.builder().id(BlockIds.SHROOMLIGHT).maxStackSize(64).diggable(true).hardness(1).resistance(5).emitLight(15).pushesOutItems(true).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType WEEPING_VINES = IntBlock.builder().id(BlockIds.WEEPING_VINES).maxStackSize(64).diggable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType CRIMSON_NYLIUM = IntBlock.builder().id(BlockIds.CRIMSON_NYLIUM).maxStackSize(64).diggable(true).hardness(0.4f).resistance(2).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: nylium type
    public static final BlockType BASALT = IntBlock.builder().id(BlockIds.BASALT).maxStackSize(64).diggable(true).hardness(1).resistance(5).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: polished type
    public static final BlockType SOUL_SOIL = IntBlock.builder().id(BlockIds.SOUL_SOIL).maxStackSize(64).diggable(true).hardness(1).resistance(5).pushesOutItems(true).solid(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType NETHER_SPROUTS = IntBlock.builder().id(BlockIds.NETHER_SPROUTS).maxStackSize(64).diggable(true).flammable(true).replaceable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType TARGET = IntBlock.builder().id(BlockIds.TARGET).maxStackSize(64).diggable(true).transparent(true).hardness(0.5f).resistance(2.5f).burnAbility(20).burnChance(60).pushesOutItems(true).solid(true).powerSource(true).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType NETHERITE_BLOCK = IntBlock.builder().id(BlockIds.NETHERITE_BLOCK).maxStackSize(64).diggable(true).hardness(50).resistance(3600).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.DIAMOND).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType ANCIENT_DEBRIS = IntBlock.builder().id(BlockIds.ANCIENT_DEBRIS).maxStackSize(64).diggable(true).hardness(30).resistance(3600).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType RESPAWN_ANCHOR = IntBlock.builder().id(BlockIds.RESPAWN_ANCHOR).maxStackSize(64).diggable(true).hardness(50).resistance(3600).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).comparatorSignal(true).breakFlowing(true).build();
    public static final BlockType BLACKSTONE = IntBlock.builder().id(BlockIds.BLACKSTONE).maxStackSize(64).diggable(true).hardness(1.5f).resistance(30).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType POLISHED_BLACKSTONE_BRICKS = IntBlock.builder().id(BlockIds.POLISHED_BLACKSTONE_BRICKS).maxStackSize(64).diggable(true).hardness(1.5f).resistance(30).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: cracked type
    public static final BlockType POLISHED_BLACKSTONE = IntBlock.builder().id(BlockIds.POLISHED_BLACKSTONE).maxStackSize(64).diggable(true).resistance(30).hardness(1.5f).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build(); //TODO: chiseled type
    public static final BlockType GILDED_BLACKSTONE = IntBlock.builder().id(BlockIds.GILDED_BLACKSTONE).maxStackSize(64).diggable(true).hardness(1.5f).resistance(30).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CHAIN = IntBlock.builder().id(BlockIds.CHAIN).maxStackSize(64).diggable(true).hardness(5).resistance(15).translucency(0.8f).targetTool(ToolTypes.PICKAXE).filterLight(0).breakFalling(true).canBeSilkTouched(true).breakFlowing(true).waterlogSource(true).build();
    public static final BlockType TWISTING_VINES = IntBlock.builder().id(BlockIds.TWISTING_VINES).maxStackSize(64).diggable(true).translucency(1).targetTool(ToolTypes.AXE).filterLight(0).breakFalling(true).blockWater(false).canBeSilkTouched(true).blockSolid(false).blockMotion(false).breakFlowing(true).build();
    public static final BlockType NETHER_GOLD_ORE = IntBlock.builder().id(BlockIds.NETHER_GOLD_ORE).maxStackSize(64).diggable(true).hardness(3).resistance(15).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.IRON).canBeSilkTouched(true).breakFlowing(true).build();
    public static final BlockType CRYING_OBSIDIAN = IntBlock.builder().id(BlockIds.CRYING_OBSIDIAN).maxStackSize(64).diggable(true).hardness(35).resistance(6000).emitLight(9).pushesOutItems(true).solid(true).targetTool(ToolTypes.PICKAXE).toolTier(TierTypes.DIAMOND).canBeSilkTouched(true).breakFlowing(true).build();

    public static BlockType byId(Identifier id) {
        BlockType type = BY_ID.get(id);
        if (type == null) {
            throw new IllegalArgumentException("ID " + id + " is not valid.");
        }
        return type;
    }

    @Builder(builderClassName = "IntBlockBuilder")
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
        private final int burnAbility;
        private final float resistance;
        private final int fuelTime;
        private final float friction;
        private final boolean pushesOutItems;
        private final boolean fallable;
        private final boolean experimental;
        private final boolean replaceable;
        private final float translucency;
        private final boolean powerSource;
        private final boolean breakFalling;
        private final boolean blockWater;
        private final boolean canBeSilkTouched;
        private final boolean blockSolid;
        private final boolean blockMotion;
        private final boolean comparatorSignal;
        private final boolean pushUpFalling;
        private final boolean breakFlowing;
        private final boolean waterlogSource;
        private final ToolType targetTool;
        private final TierType toolTier;

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
                int burnAbility,
                float resistance,
                int fuelTime,
                float friction,
                boolean pushesOutItems,
                boolean fallable,
                boolean experimental,
                boolean replaceable,
                int translucency,
                boolean powerSource,
                boolean breakFalling,
                boolean blockWater,
                boolean canBeSilkTouched,
                boolean blockSolid,
                boolean blockMotion,
                boolean comparatorSignal,
                boolean pushUpFalling,
                boolean stayInFlowing,
                boolean waterlogSource,
                ToolType targetTool,
                TierType toolTier
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
            this.burnAbility = burnAbility;
            this.resistance = resistance;
            this.fuelTime = fuelTime;
            this.friction = friction;
            this.pushesOutItems = pushesOutItems;
            this.fallable = fallable;
            this.experimental = experimental;
            this.replaceable = replaceable;
            this.translucency = translucency;
            this.targetTool = targetTool;
            this.toolTier = toolTier;
            this.powerSource = powerSource;
            this.breakFalling = breakFalling;
            this.blockWater = blockWater;
            this.canBeSilkTouched = canBeSilkTouched;
            this.blockSolid = blockSolid;
            this.blockMotion = blockMotion;
            this.comparatorSignal = comparatorSignal;
            this.pushUpFalling = pushUpFalling;
            this.breakFlowing = !stayInFlowing;
            this.waterlogSource = waterlogSource;

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
        public float getHardness() {
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
        public int getBurnChance() {
            return burnChance;
        }

        @Override
        public int getBurnAbility() {
            return burnAbility;
        }

        @Override
        public float getResistance() {
            return resistance;
        }

        @Override
        public float getTranslucency() {
            return translucency;
        }

        @Override
        public int getFuelTime() {
            return fuelTime;
        }

        @Override
        public float getFriction() {
            return friction;
        }

        @Override
        public boolean pushesOutItems() {
            return pushesOutItems;
        }

        @Override
        public boolean isFallable() {
            return fallable;
        }

        @Override
        public boolean isExperimental() {
            return experimental;
        }

        @Override
        public boolean isReplaceable() {
            return replaceable;
        }

        @Override
        public boolean isPowerSource() {
            return powerSource;
        }

        @Override
        public boolean breaksFalling() {
            return breakFalling;
        }

        @Override
        public boolean blocksWater() {
            return blockWater;
        }

        @Override
        public boolean canBeSilkTouched() {
            return canBeSilkTouched;
        }

        @Override
        public boolean blocksSolid() {
            return blockSolid;
        }

        @Override
        public boolean blocksMotion() {
            return blockMotion;
        }

        @Override
        public boolean hasComparatorSignal() {
            return comparatorSignal;
        }

        @Override
        public boolean pushesUpFalling() {
            return pushUpFalling;
        }

        @Override
        public boolean breaksFlowing() {
            return breakFlowing;
        }

        @Override
        public boolean waterlogsSource() {
            return waterlogSource;
        }

        @Nullable
        @Override
        public Class<?> getMetadataClass() {
            return null;
        }

        @Override
        public ToolType getTargetToolType() {
            return this.targetTool;
        }

        @Override
        public TierType getToolTier() {
            return toolTier;
        }

        @Override
        public String toString() {
            return "BlockType(" + id + ")";
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public static class IntBlockBuilder {

            private float friction = 0.6f;
            private int filterLight = 15;
            private boolean blockWater = true;
            private boolean blockSolid = true;
            private boolean blockMotion = true;
        }
    }
}
