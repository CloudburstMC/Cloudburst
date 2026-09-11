package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.level.Sound;

import java.util.List;

import static org.cloudburstmc.api.block.BlockTypes.*;

@UtilityClass
public class BlockTypeInitializer {

    public static void init(CloudBlockRegistry registry) {
        registerWoodenButton(registry, ACACIA_BUTTON);
        registerDoor(registry, ACACIA_DOOR);
        registerFenceGate(registry, ACACIA_FENCE_GATE);
        registerHangingSign(registry, ACACIA_HANGING_SIGN);
        registerLeaves(registry, ACACIA_LEAVES, VanillaBlockLoot.leaves(ACACIA_LEAVES, ACACIA_SAPLING, false));
        registry.configure(ACACIA_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(ACACIA_DOUBLE_SLAB));
        registerStairs(registry, ACACIA_STAIRS);
        registerTrapdoor(registry, ACACIA_TRAPDOOR);
        registerPoweredRail(registry, ACTIVATOR_RAIL);
        registry.configure(AIR).set(BlockComponents.GET_LOOT, (block, context) -> List.of());
        registry.configure(ANDESITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(ANDESITE_DOUBLE_SLAB));
        registerStairs(registry, ANDESITE_STAIRS);
        registerStairs(registry, ANDESITE_STAIRS);
        registerLeaves(registry, AZALEA_LEAVES, VanillaBlockLoot.leaves(AZALEA_LEAVES, AZALEA, false));
        registerLeaves(
                registry,
                AZALEA_LEAVES_FLOWERED,
                VanillaBlockLoot.leaves(AZALEA_LEAVES_FLOWERED, FLOWERING_AZALEA, false));
        registerWoodenButton(registry, BAMBOO_BUTTON);
        registerDoor(registry, BAMBOO_DOOR);
        registerFenceGate(registry, BAMBOO_FENCE_GATE);
        registerHangingSign(registry, BAMBOO_HANGING_SIGN);
        registerStairs(registry, BAMBOO_MOSAIC_STAIRS);
        registry.configure(BAMBOO_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BAMBOO_DOUBLE_SLAB));
        registerStairs(registry, BAMBOO_STAIRS);
        registerTrapdoor(registry, BAMBOO_TRAPDOOR);
        registry.configure(BARREL)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BARREL);
        registry.configure(BEACON)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BEACON);
        registerBed(registry, BED);
        registerWoodenButton(registry, BIRCH_BUTTON);
        registerDoor(registry, BIRCH_DOOR);
        registerFenceGate(registry, BIRCH_FENCE_GATE);
        registerHangingSign(registry, BIRCH_HANGING_SIGN);
        registerLeaves(registry, BIRCH_LEAVES, VanillaBlockLoot.leaves(BIRCH_LEAVES, BIRCH_SAPLING, false));
        registry.configure(BIRCH_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BIRCH_DOUBLE_SLAB));
        registerStairs(registry, BIRCH_STAIRS);
        registerTrapdoor(registry, BIRCH_TRAPDOOR);
        registry.configure(BLACKSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BLACKSTONE_DOUBLE_SLAB));
        registerStairs(registry, BLACKSTONE_STAIRS);
        registerConcretePowder(registry, BLACK_CONCRETE_POWDER, BLACK_CONCRETE);
        registerShulkerBox(registry, BLACK_SHULKER_BOX);
        registerConcretePowder(registry, BLUE_CONCRETE_POWDER, BLUE_CONCRETE);
        registerShulkerBox(registry, BLUE_SHULKER_BOX);
        registerStairs(registry, BRICK_STAIRS);
        registerConcretePowder(registry, BROWN_CONCRETE_POWDER, BROWN_CONCRETE);
        registerShulkerBox(registry, BROWN_SHULKER_BOX);
        registerWoodenButton(registry, CHERRY_BUTTON);
        registerDoor(registry, CHERRY_DOOR);
        registerFenceGate(registry, CHERRY_FENCE_GATE);
        registerHangingSign(registry, CHERRY_HANGING_SIGN);
        registerLeaves(registry, CHERRY_LEAVES, VanillaBlockLoot.leaves(CHERRY_LEAVES, CHERRY_SAPLING, false));
        registerStairs(registry, CHERRY_STAIRS);
        registerTrapdoor(registry, CHERRY_TRAPDOOR);
        registerAnvil(registry, CHIPPED_ANVIL);
        registerStairs(registry, CINNABAR_BRICK_STAIRS);
        registerStairs(registry, CINNABAR_STAIRS);
        registerOre(registry, COAL_ORE, OreLoot.coal(COAL_ORE.getDefaultState()));
        registerStairs(registry, COBBLED_DEEPSLATE_STAIRS);
        registerStairs(registry, COBBLESTONE_STAIRS);
        registerTorch(registry, COLORED_TORCH_BLUE);
        registerTorch(registry, COLORED_TORCH_GREEN);
        registerTorch(registry, COLORED_TORCH_PURPLE);
        registerTorch(registry, COLORED_TORCH_RED);
        registerDoor(registry, COPPER_DOOR);
        registerOre(registry, COPPER_ORE, OreLoot.copper(COPPER_ORE.getDefaultState()));
        registerTorch(registry, COPPER_TORCH);
        registerTrapdoor(registry, COPPER_TRAPDOOR);
        registerWoodenButton(registry, CRIMSON_BUTTON);
        registerDoor(registry, CRIMSON_DOOR);
        registerFenceGate(registry, CRIMSON_FENCE_GATE);
        registerHangingSign(registry, CRIMSON_HANGING_SIGN);
        registerStairs(registry, CRIMSON_STAIRS);
        registerTrapdoor(registry, CRIMSON_TRAPDOOR);
        registerStairs(registry, CUT_COPPER_STAIRS);
        registerConcretePowder(registry, CYAN_CONCRETE_POWDER, CYAN_CONCRETE);
        registerShulkerBox(registry, CYAN_SHULKER_BOX);
        registerAnvil(registry, DAMAGED_ANVIL);
        registerWoodenButton(registry, DARK_OAK_BUTTON);
        registerDoor(registry, DARK_OAK_DOOR);
        registerFenceGate(registry, DARK_OAK_FENCE_GATE);
        registerHangingSign(registry, DARK_OAK_HANGING_SIGN);
        registerLeaves(registry, DARK_OAK_LEAVES, VanillaBlockLoot.leaves(DARK_OAK_LEAVES, DARK_OAK_SAPLING, true));
        registerStairs(registry, DARK_OAK_STAIRS);
        registerTrapdoor(registry, DARK_OAK_TRAPDOOR);
        registerStairs(registry, DARK_PRISMARINE_STAIRS);
        registerStairs(registry, DEEPSLATE_BRICK_STAIRS);
        registerOre(registry, DEEPSLATE_COAL_ORE, OreLoot.coal(DEEPSLATE_COAL_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_COPPER_ORE, OreLoot.copper(DEEPSLATE_COPPER_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_DIAMOND_ORE, OreLoot.diamond(DEEPSLATE_DIAMOND_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_EMERALD_ORE, OreLoot.emerald(DEEPSLATE_EMERALD_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_GOLD_ORE, OreLoot.gold(DEEPSLATE_GOLD_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_IRON_ORE, OreLoot.iron(DEEPSLATE_IRON_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_LAPIS_ORE, OreLoot.lapis(DEEPSLATE_LAPIS_ORE.getDefaultState()));
        registerOre(registry, DEEPSLATE_REDSTONE_ORE, OreLoot.redstone(DEEPSLATE_REDSTONE_ORE.getDefaultState()));
        registerStairs(registry, DEEPSLATE_TILE_STAIRS);
        registerPoweredRail(registry, DETECTOR_RAIL);
        registerOre(registry, DIAMOND_ORE, OreLoot.diamond(DIAMOND_ORE.getDefaultState()));
        registerStairs(registry, DIORITE_STAIRS);
        registerFalling(registry, DRAGON_EGG, Sound.LAND_STONE, Sound.DIG_STONE);
        registerOre(registry, EMERALD_ORE, OreLoot.emerald(EMERALD_ORE.getDefaultState()));
        registerStairs(registry, END_BRICK_STAIRS);
        registerDoor(registry, EXPOSED_COPPER_DOOR);
        registerTrapdoor(registry, EXPOSED_COPPER_TRAPDOOR);
        registerStairs(registry, EXPOSED_CUT_COPPER_STAIRS);
        registerLiquid(registry, FLOWING_LAVA, LiquidTypes.FLOWING_LAVA)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, LiquidBlockHandlers::randomTick)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.LAVA_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registerLiquid(registry, FLOWING_WATER, LiquidTypes.FLOWING_WATER)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block));
        registerPoweredRail(registry, GOLDEN_RAIL);
        registerOre(registry, GOLD_ORE, OreLoot.gold(GOLD_ORE.getDefaultState()));
        registerStairs(registry, GRANITE_STAIRS);
        registerFalling(registry, GRAVEL, Sound.LAND_GRAVEL, Sound.DIG_GRAVEL);
        registerConcretePowder(registry, GRAY_CONCRETE_POWDER, GRAY_CONCRETE);
        registerShulkerBox(registry, GRAY_SHULKER_BOX);
        registerConcretePowder(registry, GREEN_CONCRETE_POWDER, GREEN_CONCRETE);
        registerShulkerBox(registry, GREEN_SHULKER_BOX);
        registerDoor(registry, IRON_DOOR);
        registerOre(registry, IRON_ORE, OreLoot.iron(IRON_ORE.getDefaultState()));
        registerTrapdoor(registry, IRON_TRAPDOOR);
        registerWoodenButton(registry, JUNGLE_BUTTON);
        registerDoor(registry, JUNGLE_DOOR);
        registerFenceGate(registry, JUNGLE_FENCE_GATE);
        registerHangingSign(registry, JUNGLE_HANGING_SIGN);
        registerLeaves(registry, JUNGLE_LEAVES, VanillaBlockLoot.jungleLeaves(JUNGLE_LEAVES, JUNGLE_SAPLING));
        registerStairs(registry, JUNGLE_STAIRS);
        registerTrapdoor(registry, JUNGLE_TRAPDOOR);
        registerOre(registry, LAPIS_ORE, OreLoot.lapis(LAPIS_ORE.getDefaultState()));
        registerLiquid(registry, LAVA, LiquidTypes.LAVA)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, LiquidBlockHandlers::randomTick)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.LAVA_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registerConcretePowder(registry, LIGHT_BLUE_CONCRETE_POWDER, LIGHT_BLUE_CONCRETE);
        registerShulkerBox(registry, LIGHT_BLUE_SHULKER_BOX);
        registerConcretePowder(registry, LIGHT_GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE);
        registerShulkerBox(registry, LIGHT_GRAY_SHULKER_BOX);
        registerConcretePowder(registry, LIME_CONCRETE_POWDER, LIME_CONCRETE);
        registerShulkerBox(registry, LIME_SHULKER_BOX);
        registerOre(registry, LIT_DEEPSLATE_REDSTONE_ORE, OreLoot.redstone(DEEPSLATE_REDSTONE_ORE.getDefaultState()));
        registerOre(registry, LIT_REDSTONE_ORE, OreLoot.redstone(REDSTONE_ORE.getDefaultState()));
        registerConcretePowder(registry, MAGENTA_CONCRETE_POWDER, MAGENTA_CONCRETE);
        registerShulkerBox(registry, MAGENTA_SHULKER_BOX);
        registerWoodenButton(registry, MANGROVE_BUTTON);
        registerDoor(registry, MANGROVE_DOOR);
        registerFenceGate(registry, MANGROVE_FENCE_GATE);
        registerHangingSign(registry, MANGROVE_HANGING_SIGN);
        registerLeaves(registry, MANGROVE_LEAVES, VanillaBlockLoot.mangroveLeaves(MANGROVE_LEAVES));
        registerStairs(registry, MANGROVE_STAIRS);
        registerTrapdoor(registry, MANGROVE_TRAPDOOR);
        registerStairs(registry, MOSSY_COBBLESTONE_STAIRS);
        registerStairs(registry, MOSSY_STONE_BRICK_STAIRS);
        registerStairs(registry, MUD_BRICK_STAIRS);
        registerStairs(registry, NETHER_BRICK_STAIRS);
        registerOre(registry, NETHER_GOLD_ORE, OreLoot.netherGold(NETHER_GOLD_ORE.getDefaultState()));
        registerWoodenButton(registry, OAK_BUTTON);
        registerDoor(registry, OAK_DOOR);
        registerFenceGate(registry, OAK_FENCE_GATE);
        registerHangingSign(registry, OAK_HANGING_SIGN);
        registerLeaves(registry, OAK_LEAVES, VanillaBlockLoot.leaves(OAK_LEAVES, OAK_SAPLING, true));
        registerStairs(registry, OAK_STAIRS);
        registerTrapdoor(registry, OAK_TRAPDOOR);
        registerConcretePowder(registry, ORANGE_CONCRETE_POWDER, ORANGE_CONCRETE);
        registerLeaves(
                registry, ORANGE_POPLAR_LEAVES, VanillaBlockLoot.leaves(ORANGE_POPLAR_LEAVES, POPLAR_SAPLING, false));
        registerShulkerBox(registry, ORANGE_SHULKER_BOX);
        registerDoor(registry, OXIDIZED_COPPER_DOOR);
        registerTrapdoor(registry, OXIDIZED_COPPER_TRAPDOOR);
        registerStairs(registry, OXIDIZED_CUT_COPPER_STAIRS);
        registerWoodenButton(registry, PALE_OAK_BUTTON);
        registerDoor(registry, PALE_OAK_DOOR);
        registerFenceGate(registry, PALE_OAK_FENCE_GATE);
        registerHangingSign(registry, PALE_OAK_HANGING_SIGN);
        registerLeaves(registry, PALE_OAK_LEAVES, VanillaBlockLoot.leaves(PALE_OAK_LEAVES, PALE_OAK_SAPLING, false));
        registerStairs(registry, PALE_OAK_STAIRS);
        registerTrapdoor(registry, PALE_OAK_TRAPDOOR);
        registerConcretePowder(registry, PINK_CONCRETE_POWDER, PINK_CONCRETE);
        registerShulkerBox(registry, PINK_SHULKER_BOX);
        registerWoodenButton(registry, POPLAR_BUTTON);
        registerDoor(registry, POPLAR_DOOR);
        registerFenceGate(registry, POPLAR_FENCE_GATE);
        registerHangingSign(registry, POPLAR_HANGING_SIGN);
        registerStairs(registry, POPLAR_STAIRS);
        ;
        registerTrapdoor(registry, POPLAR_TRAPDOOR);
        registerStairs(registry, POLISHED_ANDESITE_STAIRS);
        registerStairs(registry, POLISHED_BLACKSTONE_BRICK_STAIRS);
        registerStoneButton(registry, POLISHED_BLACKSTONE_BUTTON);
        registerStairs(registry, POLISHED_BLACKSTONE_STAIRS);
        registerStairs(registry, POLISHED_CINNABAR_STAIRS);
        registerStairs(registry, POLISHED_DEEPSLATE_STAIRS);
        registerStairs(registry, POLISHED_DIORITE_STAIRS);
        registerStairs(registry, POLISHED_GRANITE_STAIRS);
        registerStairs(registry, POLISHED_SULFUR_STAIRS);
        registerStairs(registry, POLISHED_TUFF_STAIRS);
        registerStairs(registry, PRISMARINE_BRICKS_STAIRS);
        registerStairs(registry, PRISMARINE_STAIRS);
        registerConcretePowder(registry, PURPLE_CONCRETE_POWDER, PURPLE_CONCRETE);
        registerShulkerBox(registry, PURPLE_SHULKER_BOX);
        registerStairs(registry, PURPUR_STAIRS);
        registerOre(registry, QUARTZ_ORE, OreLoot.quartz(QUARTZ_ORE.getDefaultState()));
        registerStairs(registry, QUARTZ_STAIRS);
        registerRail(registry, RAIL);
        registerOre(registry, REDSTONE_ORE, OreLoot.redstone(REDSTONE_ORE.getDefaultState()));
        registerTorch(registry, REDSTONE_TORCH);
        registerConcretePowder(registry, RED_CONCRETE_POWDER, RED_CONCRETE);
        registerLeaves(registry, RED_POPLAR_LEAVES, VanillaBlockLoot.leaves(RED_POPLAR_LEAVES, POPLAR_SAPLING, false));
        registerStairs(registry, RED_NETHER_BRICK_STAIRS);
        registerFalling(registry, RED_SAND, Sound.LAND_SAND, Sound.DIG_SAND);
        registerStairs(registry, RED_SANDSTONE_STAIRS);
        registerShulkerBox(registry, RED_SHULKER_BOX);
        registerStairs(registry, RESIN_BRICK_STAIRS);
        registerFalling(registry, SAND, Sound.LAND_SAND, Sound.DIG_SAND);
        registerStairs(registry, SANDSTONE_STAIRS);
        registerStairs(registry, SMOOTH_QUARTZ_STAIRS);
        registerStairs(registry, SMOOTH_RED_SANDSTONE_STAIRS);
        registerStairs(registry, SMOOTH_SANDSTONE_STAIRS);
        registerFalling(registry, SNOW_LAYER, Sound.LAND_SNOW, Sound.DIG_SNOW)
                .set(BlockComponents.CAN_BE_REPLACED, SnowLayerBlockHandlers.CAN_BE_REPLACED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.CAN_SURVIVE, SnowLayerBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.SNOW_LAYER_BLOCK_SUPPORT_SHAPE)
                .set(
                        BlockComponents.GET_LOOT,
                        (block, context) -> List.of(SnowLayerBlockHandlers.getResource(block.getState())))
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, SnowLayerBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.ON_RANDOM_TICK, SnowLayerBlockHandlers.ON_RANDOM_TICK);
        registerTorch(registry, SOUL_TORCH);
        registerWoodenButton(registry, SPRUCE_BUTTON);
        registerDoor(registry, SPRUCE_DOOR);
        registerFenceGate(registry, SPRUCE_FENCE_GATE);
        registerHangingSign(registry, SPRUCE_HANGING_SIGN);
        registerLeaves(registry, SPRUCE_LEAVES, VanillaBlockLoot.leaves(SPRUCE_LEAVES, SPRUCE_SAPLING, false));
        registerStairs(registry, SPRUCE_STAIRS);
        registerTrapdoor(registry, SPRUCE_TRAPDOOR);
        registerStairs(registry, STONE_BRICK_STAIRS);
        registerStoneButton(registry, STONE_BUTTON);
        registerStairs(registry, STONE_STAIRS);
        registerBed(registry, STRAW_BED);
        registerStairs(registry, SULFUR_BRICK_STAIRS);
        registerStairs(registry, SULFUR_STAIRS);
        registerTorch(registry, TORCH);
        registerStairs(registry, TUFF_BRICK_STAIRS);
        registerStairs(registry, TUFF_STAIRS);
        registerTorch(registry, UNDERWATER_TORCH);
        registerShulkerBox(registry, UNDYED_SHULKER_BOX);
        registerTorch(registry, UNLIT_REDSTONE_TORCH);
        registerWoodenButton(registry, WARPED_BUTTON);
        registerDoor(registry, WARPED_DOOR);
        registerFenceGate(registry, WARPED_FENCE_GATE);
        registerHangingSign(registry, WARPED_HANGING_SIGN);
        registerStairs(registry, WARPED_STAIRS);
        registerTrapdoor(registry, WARPED_TRAPDOOR);
        registerLiquid(registry, WATER, LiquidTypes.WATER)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block));
        registerDoor(registry, WAXED_COPPER_DOOR);
        registerTrapdoor(registry, WAXED_COPPER_TRAPDOOR);
        registerStairs(registry, WAXED_CUT_COPPER_STAIRS);
        registerDoor(registry, WAXED_EXPOSED_COPPER_DOOR);
        registerTrapdoor(registry, WAXED_EXPOSED_COPPER_TRAPDOOR);
        registerStairs(registry, WAXED_EXPOSED_CUT_COPPER_STAIRS);
        registerDoor(registry, WAXED_OXIDIZED_COPPER_DOOR);
        registerTrapdoor(registry, WAXED_OXIDIZED_COPPER_TRAPDOOR);
        registerStairs(registry, WAXED_OXIDIZED_CUT_COPPER_STAIRS);
        registerDoor(registry, WAXED_WEATHERED_COPPER_DOOR);
        registerTrapdoor(registry, WAXED_WEATHERED_COPPER_TRAPDOOR);
        registerStairs(registry, WAXED_WEATHERED_CUT_COPPER_STAIRS);
        registerDoor(registry, WEATHERED_COPPER_DOOR);
        registerTrapdoor(registry, WEATHERED_COPPER_TRAPDOOR);
        registerStairs(registry, WEATHERED_CUT_COPPER_STAIRS);
        registerConcretePowder(registry, WHITE_CONCRETE_POWDER, WHITE_CONCRETE);
        registerShulkerBox(registry, WHITE_SHULKER_BOX);
        registerConcretePowder(registry, YELLOW_CONCRETE_POWDER, YELLOW_CONCRETE);
        registerLeaves(
                registry, YELLOW_POPLAR_LEAVES, VanillaBlockLoot.leaves(YELLOW_POPLAR_LEAVES, POPLAR_SAPLING, false));
        registerShulkerBox(registry, YELLOW_SHULKER_BOX);
        registry.configure(BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BRICK_DOUBLE_SLAB));
        registry.configure(BREWING_STAND)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BREWING_STAND);
        registry.configure(BLAST_FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BLAST_FURNACE);
        registry.configure(BUBBLE_COLUMN)
                .set(BlockComponents.CAN_BE_REPLACED, (block, replacement, player, face, click) -> true)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> BubbleColumnBlockHandlers.update(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, BubbleColumnBlockHandlers.ON_ENTITY_INSIDE);
        registry.configure(BUSH).set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOrSilkTouchOnly(BUSH));
        registry.configure(CACTUS).set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.CACTUS_ENTITY_INSIDE);
        registry.configure(CARTOGRAPHY_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CARTOGRAPHY_TABLE);
        registry.configure(CARVED_PUMPKIN).set(BlockComponents.ON_PLACE, new CarvedPumpkinPlaceHandler(registry));
        registry.configure(CHERRY_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CHERRY_DOUBLE_SLAB));
        registry.configure(CHEST)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CHEST);
        registry.configure(CHORUS_FLOWER)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.CHORUS_FLOWER_BLOCK_SUPPORT_SHAPE);
        registry.configure(CINNABAR_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CINNABAR_BRICK_DOUBLE_SLAB));
        registry.configure(CINNABAR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CINNABAR_DOUBLE_SLAB));
        registry.configure(COBBLED_DEEPSLATE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(COBBLED_DEEPSLATE_DOUBLE_SLAB));
        registry.configure(COBBLESTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(COBBLESTONE_DOUBLE_SLAB));
        registry.configure(CRAFTER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CRAFTER);
        registry.configure(CRAFTING_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CRAFTING_TABLE);
        registry.configure(CRIMSON_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CRIMSON_DOUBLE_SLAB));
        registry.configure(CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DOUBLE_CUT_COPPER_SLAB));
        registry.configure(CUT_RED_SANDSTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CUT_RED_SANDSTONE_DOUBLE_SLAB));
        registry.configure(CUT_SANDSTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CUT_SANDSTONE_DOUBLE_SLAB));
        registry.configure(DARK_OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DARK_OAK_DOUBLE_SLAB));
        registry.configure(DARK_PRISMARINE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DARK_PRISMARINE_DOUBLE_SLAB));
        registry.configure(DEEPSLATE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(DEEPSLATE, COBBLED_DEEPSLATE));
        registry.configure(DEEPSLATE_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DEEPSLATE_BRICK_DOUBLE_SLAB));
        registry.configure(DEEPSLATE_TILE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DEEPSLATE_TILE_DOUBLE_SLAB));
        registry.configure(DIORITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DIORITE_DOUBLE_SLAB));
        registry.configure(DISPENSER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.DISPENSER);
        registry.configure(DROPPER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.DROPPER);
        registry.configure(END_STONE_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(END_STONE_BRICK_DOUBLE_SLAB));
        registry.configure(EXPOSED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(EXPOSED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(FIRE)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.FIRE_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registry.configure(FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.FURNACE);
        registry.configure(GLASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(GLASS));
        registry.configure(GLOWSTONE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.glowstone(GLOWSTONE));
        registry.configure(GRASS_BLOCK)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(GRASS_BLOCK, DIRT));
        registry.configure(GRASS_PATH)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(DIRT.getDefaultState())));
        registry.configure(GRINDSTONE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.GRINDSTONE);
        registry.configure(HOPPER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.HOPPER);
        registry.configure(JUNGLE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(JUNGLE_DOUBLE_SLAB));
        registry.configure(LECTERN)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, LecternBlockHandlers.USE);
        registry.configure(LEVER)
                .set(BlockComponents.ON_PLACE, new LeverPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, LeverBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, LeverBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, LeverBlockHandlers.ON_DESTROY);
        registry.configure(LIT_BLAST_FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BLAST_FURNACE);
        registry.configure(LIT_FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.FURNACE);
        registry.configure(LIT_PUMPKIN).set(BlockComponents.ON_PLACE, new CarvedPumpkinPlaceHandler(registry));
        registry.configure(LIT_SMOKER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SMOKER);
        registry.configure(LOOM)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.LOOM);
        registry.configure(MAGMA)
                .set(BlockComponents.ON_PLACE, BubbleColumnBlockHandlers.supportPlacement(registry))
                .set(
                        BlockComponents.ON_NEIGHBOUR_CHANGED,
                        (block, neighbor) -> BubbleColumnBlockHandlers.updateAbove(block));
        registry.configure(MANGROVE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MANGROVE_DOUBLE_SLAB));
        registry.configure(MOSSY_COBBLESTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MOSSY_COBBLESTONE_DOUBLE_SLAB));
        registry.configure(MUD)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FULL_BLOCK_SUPPORT_SHAPE);
        registry.configure(MUD_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MUD_BRICK_DOUBLE_SLAB));
        registry.configure(MYCELIUM)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(MYCELIUM, DIRT));
        registry.configure(NETHER_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(NETHER_BRICK_DOUBLE_SLAB));
        registry.configure(OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(OAK_DOUBLE_SLAB));
        registry.configure(OXIDIZED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(OXIDIZED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(PALE_OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PALE_OAK_DOUBLE_SLAB));
        registry.configure(PETRIFIED_OAK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PETRIFIED_OAK_DOUBLE_SLAB));
        registry.configure(PODZOL).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(PODZOL, DIRT));
        registry.configure(POPLAR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POPLAR_DOUBLE_SLAB));
        registry.configure(POLISHED_ANDESITE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_ANDESITE_DOUBLE_SLAB));
        registry.configure(POLISHED_BLACKSTONE_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_BLACKSTONE_BRICK_DOUBLE_SLAB));
        registry.configure(POLISHED_BLACKSTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_BLACKSTONE_DOUBLE_SLAB));
        registry.configure(POLISHED_CINNABAR_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_CINNABAR_DOUBLE_SLAB));
        registry.configure(POLISHED_DEEPSLATE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_DEEPSLATE_DOUBLE_SLAB));
        registry.configure(POLISHED_DIORITE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_DIORITE_DOUBLE_SLAB));
        registry.configure(POLISHED_GRANITE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_GRANITE_DOUBLE_SLAB));
        registry.configure(POLISHED_SULFUR_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_SULFUR_DOUBLE_SLAB));
        registry.configure(POLISHED_TUFF_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_TUFF_DOUBLE_SLAB));
        registry.configure(PORTAL)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PortalBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, PortalBlockHandlers.ON_RANDOM_TICK);
        registry.configure(POWDER_SNOW)
                .set(BlockComponents.BUCKET_PICKUP, PowderSnowBlockHandlers.BUCKET_PICKUP)
                .set(BlockComponents.GET_COLLISION_SHAPE, PowderSnowBlockHandlers.COLLISION_SHAPE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of())
                .set(BlockComponents.ON_FALL_ON, PowderSnowBlockHandlers.FALL_ON)
                .set(BlockComponents.ON_ENTITY_INSIDE, PowderSnowBlockHandlers.ENTITY_INSIDE);
        registry.configure(POWERED_COMPARATOR);
        registry.configure(PRISMARINE_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PRISMARINE_BRICK_DOUBLE_SLAB));
        registry.configure(PRISMARINE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PRISMARINE_DOUBLE_SLAB));
        registry.configure(PURPUR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PURPUR_DOUBLE_SLAB));
        registry.configure(QUARTZ_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(QUARTZ_DOUBLE_SLAB));
        registry.configure(RED_NETHER_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(RED_NETHER_BRICK_DOUBLE_SLAB));
        registry.configure(RED_SANDSTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(RED_SANDSTONE_DOUBLE_SLAB));
        registry.configure(RESIN_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(RESIN_BRICK_DOUBLE_SLAB));
        registry.configure(RESPAWN_ANCHOR)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, RespawnAnchorBlockHandlers.RESPAWN_ANCHOR);
        registry.configure(SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SANDSTONE_DOUBLE_SLAB));
        registry.configure(SHORT_GRASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.grass(SHORT_GRASS));
        registry.configure(SMITHING_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SMITHING_TABLE);
        registry.configure(SMOKER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SMOKER);
        registry.configure(SMOOTH_QUARTZ_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_QUARTZ_DOUBLE_SLAB));
        registry.configure(SMOOTH_RED_SANDSTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_RED_SANDSTONE_DOUBLE_SLAB));
        registry.configure(SMOOTH_SANDSTONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_SANDSTONE_DOUBLE_SLAB));
        registry.configure(SMOOTH_STONE_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_STONE_DOUBLE_SLAB));
        registry.configure(SNOW)
                .set(
                        BlockComponents.GET_LOOT,
                        (block, context) ->
                                List.of(ItemStack.from(ItemTypes.SNOWBALL).withCount(4)));
        registry.configure(SOUL_FIRE)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.FIRE_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registry.configure(SOUL_SAND)
                .set(BlockComponents.ON_PLACE, BubbleColumnBlockHandlers.supportPlacement(registry))
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FULL_BLOCK_SUPPORT_SHAPE)
                .set(
                        BlockComponents.ON_NEIGHBOUR_CHANGED,
                        (block, neighbor) -> BubbleColumnBlockHandlers.updateAbove(block));
        registry.configure(SPONGE)
                .set(BlockComponents.ON_PLACE, SpongeBlockHandlers.place(registry))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> SpongeBlockHandlers.absorb(block));
        registry.configure(SPRUCE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SPRUCE_DOUBLE_SLAB));
        registry.configure(STONE)
                .set(
                        BlockComponents.GET_LOOT,
                        (block, context) -> List.of(ItemStack.from(
                                context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0
                                        ? BlockTypes.STONE.getDefaultState()
                                        : BlockTypes.COBBLESTONE.getDefaultState())));
        registry.configure(STONECUTTER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.STONECUTTER);
        registry.configure(STONECUTTER_BLOCK)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.STONECUTTER);
        registry.configure(STONE_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(STONE_BRICK_DOUBLE_SLAB));
        registry.configure(STONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(STONE_DOUBLE_SLAB));
        registry.configure(SULFUR_BRICK_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SULFUR_BRICK_DOUBLE_SLAB));
        registry.configure(SULFUR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SULFUR_DOUBLE_SLAB));
        registry.configure(SWEET_BERRY_BUSH)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.SWEET_BERRY_BUSH_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registry.configure(TALL_GRASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.tallGrass());
        registry.configure(TRAPPED_CHEST)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.TRAPPED_CHEST);
        registry.configure(TRIPWIRE_HOOK)
                .set(BlockComponents.ON_PLACE, new TripwireHookPlaceHandler())
                .set(BlockComponents.ON_TICK, TripwireHookBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TripwireHookBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, TripwireHookBlockHandlers.ON_DESTROY);
        registry.configure(TRIP_WIRE)
                .set(BlockComponents.ON_PLACE, new TripwireBlockPlaceHandler())
                .set(BlockComponents.GET_LOOT, TripwireBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, TripwireBlockHandlers.GET_PICK_BLOCK)
                .set(BlockComponents.ON_ENTITY_INSIDE, TripwireBlockHandlers.ON_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE)
                .set(BlockComponents.ON_TICK, TripwireBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TripwireBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, TripwireBlockHandlers.ON_DESTROY);
        registry.configure(TUFF_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(TUFF_BRICK_DOUBLE_SLAB));
        registry.configure(TUFF_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(TUFF_DOUBLE_SLAB));
        registry.configure(VINE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOnly(VINE));
        registry.configure(WARPED_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WARPED_DOUBLE_SLAB));
        registry.configure(WAXED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(WAXED_EXPOSED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_EXPOSED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(WAXED_OXIDIZED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_OXIDIZED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(WAXED_WEATHERED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_WEATHERED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(WEATHERED_CUT_COPPER_SLAB)
                .set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WEATHERED_DOUBLE_CUT_COPPER_SLAB));
        registry.configure(WEB)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.WEB_ENTITY_INSIDE)
                .set(
                        BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE,
                        DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
    }

    private void registerFenceGate(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FENCE_GATE_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.ON_PLACE, new FenceGatePlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, FenceGateBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FenceGateBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void registerHangingSign(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.OUTLINE_BLOCK_SUPPORT_SHAPE);
    }

    private void registerLeaves(CloudBlockRegistry registry, BlockType type, BlockLootHandler loot) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.EMPTY_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_LOOT, loot);
    }

    private ComponentBuilder registerLiquid(CloudBlockRegistry registry, BlockType blockType, LiquidType liquidType) {
        BlockRegistrationAccess.bindLiquidType(blockType, liquidType);
        return registry.configure(blockType);
    }

    private void registerPoweredRail(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, RailPlaceHandler.INSTANCE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PoweredRailBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REMOVE, RailBlockHandlers.ON_REMOVE);
    }

    private void registerRail(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, RailPlaceHandler.INSTANCE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, RailBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REMOVE, RailBlockHandlers.ON_REMOVE);
    }

    private void registerShulkerBox(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.SHULKER_BOX_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_COLLISION_SHAPE, DefaultBlockHandlers.SHULKER_BOX_COLLISION_SHAPE)
                .set(BlockComponents.GET_OUTLINE_SHAPE, DefaultBlockHandlers.SHULKER_BOX_OUTLINE_SHAPE)
                .set(BlockComponents.ON_PLACE, new ShulkerBoxPlaceHandler(registry))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
    }

    private void registerStairs(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type).set(BlockComponents.ON_PLACE, new StairsPlaceHandler(registry));
    }

    private void registerStoneButton(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.STONE_PRESS_TICKS)
                .set(BlockComponents.ON_PLACE, new ButtonPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ButtonBlockHandlers.USE)
                .set(BlockComponents.ON_TICK, ButtonBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ButtonBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void registerTorch(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new TorchPlaceHandler())
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TorchBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void registerTrapdoor(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new TrapdoorPlaceHandler(registry))
                .set(BlockComponents.CAN_BE_USED, TrapdoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, TrapdoorBlockHandlers.USE);
    }

    private void registerWoodenButton(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.WOODEN_PRESS_TICKS)
                .set(BlockComponents.ON_PLACE, new ButtonPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ButtonBlockHandlers.USE)
                .set(BlockComponents.ON_TICK, ButtonBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ButtonBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void registerOre(CloudBlockRegistry registry, BlockType type, OreLoot loot) {
        registry.configure(type)
                .set(BlockComponents.GET_LOOT, (block, context) -> loot.drops(context))
                .set(BlockComponents.GET_EXPERIENCE, (block, context) -> loot.experience(context));
    }

    private void registerDoor(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new DoorPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.GET_LOOT, DoorBlockHandlers.GET_LOOT)
                .set(BlockComponents.USE, DoorBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, DoorBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, DoorBlockHandlers.ON_DESTROY);
    }

    private void registerBed(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, BedBlockHandlers.BED)
                .set(BlockComponents.ON_PLACE, BedBlockHandlers.PLACE)
                .set(BlockComponents.ON_DESTROY, BedBlockHandlers.ON_DESTROY)
                .set(BlockComponents.GET_LOOT, BedBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, BedBlockHandlers.GET_PICK_BLOCK);
    }

    private void registerConcretePowder(CloudBlockRegistry registry, BlockType powderType, BlockType concreteType) {
        registerFalling(registry, powderType, Sound.LAND_SAND, Sound.DIG_SAND)
                .set(BlockComponents.ON_FALLING_LAND, FallingBlockHandlers.solidifyConcretePowder(concreteType));
    }

    private ComponentBuilder registerFalling(
            CloudBlockRegistry registry, BlockType type, Sound landingSound, Sound breakSound) {
        return registerFalling(registry, type, landingSound, breakSound, 0, 40);
    }

    private ComponentBuilder registerFalling(
            CloudBlockRegistry registry,
            BlockType type,
            Sound landingSound,
            Sound breakSound,
            float damagePerBlock,
            int maximumDamage) {
        return registry.configure(type)
                .set(BlockComponents.IS_FREE_TO_FALL, FallingBlockHandlers.IS_FREE_TO_FALL)
                .set(
                        BlockComponents.START_FALLING,
                        FallingBlockHandlers.startFalling(landingSound, breakSound, damagePerBlock, maximumDamage))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FallingBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, FallingBlockHandlers.ON_TICK);
    }

    private void registerAnvil(CloudBlockRegistry registry, BlockType type) {
        registerFalling(registry, type, Sound.RANDOM_ANVIL_LAND, Sound.RANDOM_ANVIL_BREAK, 2, 40)
                .set(BlockComponents.ON_FALLING_LAND, FallingBlockHandlers.ANVIL_LAND)
                .set(BlockComponents.ON_PLACE, new AnvilPlaceHandler(registry))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ANVIL);
    }
}
