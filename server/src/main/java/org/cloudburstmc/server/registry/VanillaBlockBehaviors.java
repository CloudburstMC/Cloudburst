package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.level.Sound;

import java.util.List;

import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * Configures server behavior for vanilla block types.
 */
@UtilityClass
public class VanillaBlockBehaviors {
    private static final int MAXIMUM_FALL_DAMAGE = 40;

    public static void configure(CloudBlockRegistry registry) {
        configureWoodenButton(registry, ACACIA_BUTTON);
        configureDoor(registry, ACACIA_DOOR);
        configureFenceGate(registry, ACACIA_FENCE_GATE);
        configureHangingSign(registry, ACACIA_HANGING_SIGN);
        configureLeaves(registry, ACACIA_LEAVES, VanillaBlockLoot.leaves(ACACIA_LEAVES, ACACIA_SAPLING, false));
        configureSlab(registry, ACACIA_SLAB, ACACIA_DOUBLE_SLAB);
        configureStairs(registry, ACACIA_STAIRS);
        configureTrapdoor(registry, ACACIA_TRAPDOOR);
        configurePoweredRail(registry, ACTIVATOR_RAIL);
        registry.configure(AIR).set(BlockComponents.GET_LOOT, (block, context) -> List.of());
        configureSlab(registry, ANDESITE_SLAB, ANDESITE_DOUBLE_SLAB);
        configureStairs(registry, ANDESITE_STAIRS);
        configureAnvil(registry, ANVIL);
        configureLeaves(registry, AZALEA_LEAVES, VanillaBlockLoot.leaves(AZALEA_LEAVES, AZALEA, false));
        configureLeaves(registry, AZALEA_LEAVES_FLOWERED, VanillaBlockLoot.leaves(AZALEA_LEAVES_FLOWERED, FLOWERING_AZALEA, false));
        configureWoodenButton(registry, BAMBOO_BUTTON);
        configureDoor(registry, BAMBOO_DOOR);
        configureFenceGate(registry, BAMBOO_FENCE_GATE);
        configureHangingSign(registry, BAMBOO_HANGING_SIGN);
        configureSlab(registry, BAMBOO_MOSAIC_SLAB, BAMBOO_MOSAIC_DOUBLE_SLAB);
        configureStairs(registry, BAMBOO_MOSAIC_STAIRS);
        configureSlab(registry, BAMBOO_SLAB, BAMBOO_DOUBLE_SLAB);
        configureStairs(registry, BAMBOO_STAIRS);
        configureTrapdoor(registry, BAMBOO_TRAPDOOR);
        configureUsable(registry, BARREL, ContainerBlockHandlers.BARREL);
        configureUsable(registry, BEACON, ContainerBlockHandlers.BEACON);
        configureBed(registry, BED);
        configureWoodenButton(registry, BIRCH_BUTTON);
        configureDoor(registry, BIRCH_DOOR);
        configureFenceGate(registry, BIRCH_FENCE_GATE);
        configureHangingSign(registry, BIRCH_HANGING_SIGN);
        configureLeaves(registry, BIRCH_LEAVES, VanillaBlockLoot.leaves(BIRCH_LEAVES, BIRCH_SAPLING, false));
        configureSlab(registry, BIRCH_SLAB, BIRCH_DOUBLE_SLAB);
        configureStairs(registry, BIRCH_STAIRS);
        configureTrapdoor(registry, BIRCH_TRAPDOOR);
        configureConcretePowder(registry, BLACK_CONCRETE_POWDER, BLACK_CONCRETE);
        configureShulkerBox(registry, BLACK_SHULKER_BOX);
        configureSlab(registry, BLACKSTONE_SLAB, BLACKSTONE_DOUBLE_SLAB);
        configureStairs(registry, BLACKSTONE_STAIRS);
        configureUsable(registry, BLAST_FURNACE, ContainerBlockHandlers.BLAST_FURNACE);
        configureConcretePowder(registry, BLUE_CONCRETE_POWDER, BLUE_CONCRETE);
        configureShulkerBox(registry, BLUE_SHULKER_BOX);
        configureUsable(registry, BREWING_STAND, ContainerBlockHandlers.BREWING_STAND);
        configureSlab(registry, BRICK_SLAB, BRICK_DOUBLE_SLAB);
        configureStairs(registry, BRICK_STAIRS);
        configureConcretePowder(registry, BROWN_CONCRETE_POWDER, BROWN_CONCRETE);
        configureShulkerBox(registry, BROWN_SHULKER_BOX);
        registry.configure(BUBBLE_COLUMN)
                .set(BlockComponents.CAN_BE_REPLACED, (block, replacement, player, face, click) -> true)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> BubbleColumnBlockHandlers.update(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, BubbleColumnBlockHandlers.ON_ENTITY_INSIDE);
        registry.configure(BUSH).set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOrSilkTouchOnly(BUSH));
        registry.configure(CACTUS).set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.CACTUS_ENTITY_INSIDE);
        configureUsable(registry, CARTOGRAPHY_TABLE, ContainerBlockHandlers.CARTOGRAPHY_TABLE);
        registry.configure(CARVED_PUMPKIN).set(BlockComponents.ON_PLACE, new CarvedPumpkinPlaceHandler(registry));
        configureWoodenButton(registry, CHERRY_BUTTON);
        configureDoor(registry, CHERRY_DOOR);
        configureFenceGate(registry, CHERRY_FENCE_GATE);
        configureHangingSign(registry, CHERRY_HANGING_SIGN);
        configureLeaves(registry, CHERRY_LEAVES, VanillaBlockLoot.leaves(CHERRY_LEAVES, CHERRY_SAPLING, false));
        configureSlab(registry, CHERRY_SLAB, CHERRY_DOUBLE_SLAB);
        configureStairs(registry, CHERRY_STAIRS);
        configureTrapdoor(registry, CHERRY_TRAPDOOR);
        configureUsable(registry, CHEST, ContainerBlockHandlers.CHEST);
        configureAnvil(registry, CHIPPED_ANVIL);
        registry.configure(CHORUS_FLOWER)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.CHORUS_FLOWER_BLOCK_SUPPORT_SHAPE);
        configureSlab(registry, CINNABAR_BRICK_SLAB, CINNABAR_BRICK_DOUBLE_SLAB);
        configureStairs(registry, CINNABAR_BRICK_STAIRS);
        configureSlab(registry, CINNABAR_SLAB, CINNABAR_DOUBLE_SLAB);
        configureStairs(registry, CINNABAR_STAIRS);
        configureOre(registry, COAL_ORE, OreLoot.coal(COAL_ORE.getDefaultState()));
        configureSlab(registry, COBBLED_DEEPSLATE_SLAB, COBBLED_DEEPSLATE_DOUBLE_SLAB);
        configureStairs(registry, COBBLED_DEEPSLATE_STAIRS);
        configureSlab(registry, COBBLESTONE_SLAB, COBBLESTONE_DOUBLE_SLAB);
        configureStairs(registry, COBBLESTONE_STAIRS);
        configureTorch(registry, COLORED_TORCH_BLUE);
        configureTorch(registry, COLORED_TORCH_GREEN);
        configureTorch(registry, COLORED_TORCH_PURPLE);
        configureTorch(registry, COLORED_TORCH_RED);
        configureDoor(registry, COPPER_DOOR);
        configureOre(registry, COPPER_ORE, OreLoot.copper(COPPER_ORE.getDefaultState()));
        configureTorch(registry, COPPER_TORCH);
        configureTrapdoor(registry, COPPER_TRAPDOOR);
        configureUsable(registry, CRAFTER, ContainerBlockHandlers.CRAFTER);
        configureUsable(registry, CRAFTING_TABLE, ContainerBlockHandlers.CRAFTING_TABLE);
        configureWoodenButton(registry, CRIMSON_BUTTON);
        configureDoor(registry, CRIMSON_DOOR);
        configureFenceGate(registry, CRIMSON_FENCE_GATE);
        configureHangingSign(registry, CRIMSON_HANGING_SIGN);
        configureSlab(registry, CRIMSON_SLAB, CRIMSON_DOUBLE_SLAB);
        configureStairs(registry, CRIMSON_STAIRS);
        configureTrapdoor(registry, CRIMSON_TRAPDOOR);
        configureSlab(registry, CUT_COPPER_SLAB, DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, CUT_COPPER_STAIRS);
        configureSlab(registry, CUT_RED_SANDSTONE_SLAB, CUT_RED_SANDSTONE_DOUBLE_SLAB);
        configureSlab(registry, CUT_SANDSTONE_SLAB, CUT_SANDSTONE_DOUBLE_SLAB);
        configureConcretePowder(registry, CYAN_CONCRETE_POWDER, CYAN_CONCRETE);
        configureShulkerBox(registry, CYAN_SHULKER_BOX);
        configureAnvil(registry, DAMAGED_ANVIL);
        configureWoodenButton(registry, DARK_OAK_BUTTON);
        configureDoor(registry, DARK_OAK_DOOR);
        configureFenceGate(registry, DARK_OAK_FENCE_GATE);
        configureHangingSign(registry, DARK_OAK_HANGING_SIGN);
        configureLeaves(registry, DARK_OAK_LEAVES, VanillaBlockLoot.leaves(DARK_OAK_LEAVES, DARK_OAK_SAPLING, true));
        configureSlab(registry, DARK_OAK_SLAB, DARK_OAK_DOUBLE_SLAB);
        configureStairs(registry, DARK_OAK_STAIRS);
        configureTrapdoor(registry, DARK_OAK_TRAPDOOR);
        configureSlab(registry, DARK_PRISMARINE_SLAB, DARK_PRISMARINE_DOUBLE_SLAB);
        configureStairs(registry, DARK_PRISMARINE_STAIRS);
        registry.configure(DEEPSLATE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(DEEPSLATE, COBBLED_DEEPSLATE));
        configureSlab(registry, DEEPSLATE_BRICK_SLAB, DEEPSLATE_BRICK_DOUBLE_SLAB);
        configureStairs(registry, DEEPSLATE_BRICK_STAIRS);
        configureOre(registry, DEEPSLATE_COAL_ORE, OreLoot.coal(DEEPSLATE_COAL_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_COPPER_ORE, OreLoot.copper(DEEPSLATE_COPPER_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_DIAMOND_ORE, OreLoot.diamond(DEEPSLATE_DIAMOND_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_EMERALD_ORE, OreLoot.emerald(DEEPSLATE_EMERALD_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_GOLD_ORE, OreLoot.gold(DEEPSLATE_GOLD_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_IRON_ORE, OreLoot.iron(DEEPSLATE_IRON_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_LAPIS_ORE, OreLoot.lapis(DEEPSLATE_LAPIS_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_REDSTONE_ORE, OreLoot.redstone(DEEPSLATE_REDSTONE_ORE.getDefaultState()));
        configureSlab(registry, DEEPSLATE_TILE_SLAB, DEEPSLATE_TILE_DOUBLE_SLAB);
        configureStairs(registry, DEEPSLATE_TILE_STAIRS);
        configurePoweredRail(registry, DETECTOR_RAIL);
        configureOre(registry, DIAMOND_ORE, OreLoot.diamond(DIAMOND_ORE.getDefaultState()));
        configureSlab(registry, DIORITE_SLAB, DIORITE_DOUBLE_SLAB);
        configureStairs(registry, DIORITE_STAIRS);
        configureUsable(registry, DISPENSER, ContainerBlockHandlers.DISPENSER);
        configureFalling(registry, DRAGON_EGG, Sound.LAND_STONE, Sound.DIG_STONE);
        configureUsable(registry, DROPPER, ContainerBlockHandlers.DROPPER);
        configureOre(registry, EMERALD_ORE, OreLoot.emerald(EMERALD_ORE.getDefaultState()));
        configureUsable(registry, ENCHANTING_TABLE, ContainerBlockHandlers.ENCHANTING_TABLE);
        configureStairs(registry, END_BRICK_STAIRS);
        configureSlab(registry, END_STONE_BRICK_SLAB, END_STONE_BRICK_DOUBLE_SLAB);
        configureUsable(registry, ENDER_CHEST, ContainerBlockHandlers.ENDER_CHEST);
        configureDoor(registry, EXPOSED_COPPER_DOOR);
        configureTrapdoor(registry, EXPOSED_COPPER_TRAPDOOR);
        configureSlab(registry, EXPOSED_CUT_COPPER_SLAB, EXPOSED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, EXPOSED_CUT_COPPER_STAIRS);
        registry.configure(FIRE)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.FIRE_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        configureLiquid(registry, FLOWING_LAVA, LiquidTypes.FLOWING_LAVA)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, LiquidBlockHandlers::randomTick)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.LAVA_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        configureLiquid(registry, FLOWING_WATER, LiquidTypes.FLOWING_WATER)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block));
        configureUsable(registry, FURNACE, ContainerBlockHandlers.FURNACE);
        registry.configure(GLASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(GLASS));
        registry.configure(GLOWSTONE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.glowstone(GLOWSTONE));
        configureOre(registry, GOLD_ORE, OreLoot.gold(GOLD_ORE.getDefaultState()));
        configurePoweredRail(registry, GOLDEN_RAIL);
        configureSlab(registry, GRANITE_SLAB, GRANITE_DOUBLE_SLAB);
        configureStairs(registry, GRANITE_STAIRS);
        registry.configure(GRASS_BLOCK)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(GRASS_BLOCK, DIRT));
        registry.configure(GRASS_PATH)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(DIRT.getDefaultState())));
        configureFalling(registry, GRAVEL, Sound.LAND_GRAVEL, Sound.DIG_GRAVEL);
        configureConcretePowder(registry, GRAY_CONCRETE_POWDER, GRAY_CONCRETE);
        configureShulkerBox(registry, GRAY_SHULKER_BOX);
        configureConcretePowder(registry, GREEN_CONCRETE_POWDER, GREEN_CONCRETE);
        configureShulkerBox(registry, GREEN_SHULKER_BOX);
        configureUsable(registry, GRINDSTONE, ContainerBlockHandlers.GRINDSTONE);
        configureUsable(registry, HOPPER, ContainerBlockHandlers.HOPPER);
        configureDoor(registry, IRON_DOOR);
        configureOre(registry, IRON_ORE, OreLoot.iron(IRON_ORE.getDefaultState()));
        configureTrapdoor(registry, IRON_TRAPDOOR);
        configureWoodenButton(registry, JUNGLE_BUTTON);
        configureDoor(registry, JUNGLE_DOOR);
        configureFenceGate(registry, JUNGLE_FENCE_GATE);
        configureHangingSign(registry, JUNGLE_HANGING_SIGN);
        configureLeaves(registry, JUNGLE_LEAVES, VanillaBlockLoot.jungleLeaves(JUNGLE_LEAVES, JUNGLE_SAPLING));
        configureSlab(registry, JUNGLE_SLAB, JUNGLE_DOUBLE_SLAB);
        configureStairs(registry, JUNGLE_STAIRS);
        configureTrapdoor(registry, JUNGLE_TRAPDOOR);
        configureOre(registry, LAPIS_ORE, OreLoot.lapis(LAPIS_ORE.getDefaultState()));
        configureLiquid(registry, LAVA, LiquidTypes.LAVA)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, LiquidBlockHandlers::randomTick)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.LAVA_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        configureUsable(registry, LECTERN, LecternBlockHandlers.USE);
        registry.configure(LEVER)
                .set(BlockComponents.ON_PLACE, new LeverPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, LeverBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, LeverBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, LeverBlockHandlers.ON_DESTROY);
        configureConcretePowder(registry, LIGHT_BLUE_CONCRETE_POWDER, LIGHT_BLUE_CONCRETE);
        configureShulkerBox(registry, LIGHT_BLUE_SHULKER_BOX);
        configureConcretePowder(registry, LIGHT_GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE);
        configureShulkerBox(registry, LIGHT_GRAY_SHULKER_BOX);
        configureConcretePowder(registry, LIME_CONCRETE_POWDER, LIME_CONCRETE);
        configureShulkerBox(registry, LIME_SHULKER_BOX);
        configureUsable(registry, LIT_BLAST_FURNACE, ContainerBlockHandlers.BLAST_FURNACE);
        configureOre(registry, LIT_DEEPSLATE_REDSTONE_ORE, OreLoot.redstone(DEEPSLATE_REDSTONE_ORE.getDefaultState()));
        configureUsable(registry, LIT_FURNACE, ContainerBlockHandlers.FURNACE);
        registry.configure(LIT_PUMPKIN).set(BlockComponents.ON_PLACE, new CarvedPumpkinPlaceHandler(registry));
        configureOre(registry, LIT_REDSTONE_ORE, OreLoot.redstone(REDSTONE_ORE.getDefaultState()));
        configureUsable(registry, LIT_SMOKER, ContainerBlockHandlers.SMOKER);
        configureUsable(registry, LOOM, ContainerBlockHandlers.LOOM);
        configureConcretePowder(registry, MAGENTA_CONCRETE_POWDER, MAGENTA_CONCRETE);
        configureShulkerBox(registry, MAGENTA_SHULKER_BOX);
        registry.configure(MAGMA)
                .set(BlockComponents.ON_PLACE, BubbleColumnBlockHandlers.supportPlacement(registry))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> BubbleColumnBlockHandlers.updateAbove(block));
        configureWoodenButton(registry, MANGROVE_BUTTON);
        configureDoor(registry, MANGROVE_DOOR);
        configureFenceGate(registry, MANGROVE_FENCE_GATE);
        configureHangingSign(registry, MANGROVE_HANGING_SIGN);
        configureLeaves(registry, MANGROVE_LEAVES, VanillaBlockLoot.mangroveLeaves(MANGROVE_LEAVES));
        configureSlab(registry, MANGROVE_SLAB, MANGROVE_DOUBLE_SLAB);
        configureStairs(registry, MANGROVE_STAIRS);
        configureTrapdoor(registry, MANGROVE_TRAPDOOR);
        configureSlab(registry, MOSSY_COBBLESTONE_SLAB, MOSSY_COBBLESTONE_DOUBLE_SLAB);
        configureStairs(registry, MOSSY_COBBLESTONE_STAIRS);
        configureSlab(registry, MOSSY_STONE_BRICK_SLAB, MOSSY_STONE_BRICK_DOUBLE_SLAB);
        configureStairs(registry, MOSSY_STONE_BRICK_STAIRS);
        registry.configure(MUD)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FULL_BLOCK_SUPPORT_SHAPE);
        configureSlab(registry, MUD_BRICK_SLAB, MUD_BRICK_DOUBLE_SLAB);
        configureStairs(registry, MUD_BRICK_STAIRS);
        registry.configure(MYCELIUM)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(MYCELIUM, DIRT));
        configureSlab(registry, NETHER_BRICK_SLAB, NETHER_BRICK_DOUBLE_SLAB);
        configureStairs(registry, NETHER_BRICK_STAIRS);
        configureOre(registry, NETHER_GOLD_ORE, OreLoot.netherGold(NETHER_GOLD_ORE.getDefaultState()));
        configureWoodenButton(registry, OAK_BUTTON);
        configureDoor(registry, OAK_DOOR);
        configureFenceGate(registry, OAK_FENCE_GATE);
        configureHangingSign(registry, OAK_HANGING_SIGN);
        configureLeaves(registry, OAK_LEAVES, VanillaBlockLoot.leaves(OAK_LEAVES, OAK_SAPLING, true));
        configureSlab(registry, OAK_SLAB, OAK_DOUBLE_SLAB);
        configureStairs(registry, OAK_STAIRS);
        configureTrapdoor(registry, OAK_TRAPDOOR);
        configureConcretePowder(registry, ORANGE_CONCRETE_POWDER, ORANGE_CONCRETE);
        configureLeaves(registry, ORANGE_POPLAR_LEAVES, VanillaBlockLoot.leaves(ORANGE_POPLAR_LEAVES, POPLAR_SAPLING, false));
        configureShulkerBox(registry, ORANGE_SHULKER_BOX);
        configureDoor(registry, OXIDIZED_COPPER_DOOR);
        configureTrapdoor(registry, OXIDIZED_COPPER_TRAPDOOR);
        configureSlab(registry, OXIDIZED_CUT_COPPER_SLAB, OXIDIZED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, OXIDIZED_CUT_COPPER_STAIRS);
        configureWoodenButton(registry, PALE_OAK_BUTTON);
        configureDoor(registry, PALE_OAK_DOOR);
        configureFenceGate(registry, PALE_OAK_FENCE_GATE);
        configureHangingSign(registry, PALE_OAK_HANGING_SIGN);
        configureLeaves(registry, PALE_OAK_LEAVES, VanillaBlockLoot.leaves(PALE_OAK_LEAVES, PALE_OAK_SAPLING, false));
        configureSlab(registry, PALE_OAK_SLAB, PALE_OAK_DOUBLE_SLAB);
        configureStairs(registry, PALE_OAK_STAIRS);
        configureTrapdoor(registry, PALE_OAK_TRAPDOOR);
        configureSlab(registry, PETRIFIED_OAK_SLAB, PETRIFIED_OAK_DOUBLE_SLAB);
        configureConcretePowder(registry, PINK_CONCRETE_POWDER, PINK_CONCRETE);
        configureShulkerBox(registry, PINK_SHULKER_BOX);
        registry.configure(PODZOL).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(PODZOL, DIRT));
        configureSlab(registry, POLISHED_ANDESITE_SLAB, POLISHED_ANDESITE_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_ANDESITE_STAIRS);
        configureSlab(registry, POLISHED_BLACKSTONE_BRICK_SLAB, POLISHED_BLACKSTONE_BRICK_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_BLACKSTONE_BRICK_STAIRS);
        configureStoneButton(registry, POLISHED_BLACKSTONE_BUTTON);
        configureSlab(registry, POLISHED_BLACKSTONE_SLAB, POLISHED_BLACKSTONE_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_BLACKSTONE_STAIRS);
        configureSlab(registry, POLISHED_CINNABAR_SLAB, POLISHED_CINNABAR_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_CINNABAR_STAIRS);
        configureSlab(registry, POLISHED_DEEPSLATE_SLAB, POLISHED_DEEPSLATE_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_DEEPSLATE_STAIRS);
        configureSlab(registry, POLISHED_DIORITE_SLAB, POLISHED_DIORITE_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_DIORITE_STAIRS);
        configureSlab(registry, POLISHED_GRANITE_SLAB, POLISHED_GRANITE_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_GRANITE_STAIRS);
        configureSlab(registry, POLISHED_SULFUR_SLAB, POLISHED_SULFUR_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_SULFUR_STAIRS);
        configureSlab(registry, POLISHED_TUFF_SLAB, POLISHED_TUFF_DOUBLE_SLAB);
        configureStairs(registry, POLISHED_TUFF_STAIRS);
        configureWoodenButton(registry, POPLAR_BUTTON);
        configureDoor(registry, POPLAR_DOOR);
        configureFenceGate(registry, POPLAR_FENCE_GATE);
        configureHangingSign(registry, POPLAR_HANGING_SIGN);
        configureSlab(registry, POPLAR_SLAB, POPLAR_DOUBLE_SLAB);
        configureStairs(registry, POPLAR_STAIRS);
        configureTrapdoor(registry, POPLAR_TRAPDOOR);
        registry.configure(PORTAL)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PortalBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, PortalBlockHandlers.ON_RANDOM_TICK);
        registry.configure(POWDER_SNOW)
                .set(BlockComponents.BUCKET_PICKUP, PowderSnowBlockHandlers.BUCKET_PICKUP)
                .set(BlockComponents.GET_COLLISION_SHAPE, PowderSnowBlockHandlers.COLLISION_SHAPE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of())
                .set(BlockComponents.ON_FALL_ON, PowderSnowBlockHandlers.FALL_ON)
                .set(BlockComponents.ON_ENTITY_INSIDE, PowderSnowBlockHandlers.ENTITY_INSIDE);
        configureSlab(registry, PRISMARINE_BRICK_SLAB, PRISMARINE_BRICK_DOUBLE_SLAB);
        configureStairs(registry, PRISMARINE_BRICKS_STAIRS);
        configureSlab(registry, PRISMARINE_SLAB, PRISMARINE_DOUBLE_SLAB);
        configureStairs(registry, PRISMARINE_STAIRS);
        configureConcretePowder(registry, PURPLE_CONCRETE_POWDER, PURPLE_CONCRETE);
        configureShulkerBox(registry, PURPLE_SHULKER_BOX);
        configureSlab(registry, PURPUR_SLAB, PURPUR_DOUBLE_SLAB);
        configureStairs(registry, PURPUR_STAIRS);
        configureOre(registry, QUARTZ_ORE, OreLoot.quartz(QUARTZ_ORE.getDefaultState()));
        configureSlab(registry, QUARTZ_SLAB, QUARTZ_DOUBLE_SLAB);
        configureStairs(registry, QUARTZ_STAIRS);
        registry.configure(RAIL)
                .set(BlockComponents.ON_PLACE, RailPlaceHandler.INSTANCE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, RailBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REMOVE, RailBlockHandlers.ON_REMOVE);
        configureConcretePowder(registry, RED_CONCRETE_POWDER, RED_CONCRETE);
        configureSlab(registry, RED_NETHER_BRICK_SLAB, RED_NETHER_BRICK_DOUBLE_SLAB);
        configureStairs(registry, RED_NETHER_BRICK_STAIRS);
        configureLeaves(registry, RED_POPLAR_LEAVES, VanillaBlockLoot.leaves(RED_POPLAR_LEAVES, POPLAR_SAPLING, false));
        configureFalling(registry, RED_SAND, Sound.LAND_SAND, Sound.DIG_SAND);
        configureSlab(registry, RED_SANDSTONE_SLAB, RED_SANDSTONE_DOUBLE_SLAB);
        configureStairs(registry, RED_SANDSTONE_STAIRS);
        configureShulkerBox(registry, RED_SHULKER_BOX);
        configureOre(registry, REDSTONE_ORE, OreLoot.redstone(REDSTONE_ORE.getDefaultState()));
        configureTorch(registry, REDSTONE_TORCH);
        configureSlab(registry, RESIN_BRICK_SLAB, RESIN_BRICK_DOUBLE_SLAB);
        configureStairs(registry, RESIN_BRICK_STAIRS);
        configureUsable(registry, RESPAWN_ANCHOR, RespawnAnchorBlockHandlers.RESPAWN_ANCHOR);
        configureFalling(registry, SAND, Sound.LAND_SAND, Sound.DIG_SAND);
        configureSlab(registry, SANDSTONE_SLAB, SANDSTONE_DOUBLE_SLAB);
        configureStairs(registry, SANDSTONE_STAIRS);
        registry.configure(SHORT_GRASS)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.grass(SHORT_GRASS));
        configureUsable(registry, SMITHING_TABLE, ContainerBlockHandlers.SMITHING_TABLE);
        configureUsable(registry, SMOKER, ContainerBlockHandlers.SMOKER);
        configureSlab(registry, SMOOTH_QUARTZ_SLAB, SMOOTH_QUARTZ_DOUBLE_SLAB);
        configureStairs(registry, SMOOTH_QUARTZ_STAIRS);
        configureSlab(registry, SMOOTH_RED_SANDSTONE_SLAB, SMOOTH_RED_SANDSTONE_DOUBLE_SLAB);
        configureStairs(registry, SMOOTH_RED_SANDSTONE_STAIRS);
        configureSlab(registry, SMOOTH_SANDSTONE_SLAB, SMOOTH_SANDSTONE_DOUBLE_SLAB);
        configureStairs(registry, SMOOTH_SANDSTONE_STAIRS);
        configureSlab(registry, SMOOTH_STONE_SLAB, SMOOTH_STONE_DOUBLE_SLAB);
        registry.configure(SNOW)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(ItemTypes.SNOWBALL).withCount(4)));
        configureFalling(registry, SNOW_LAYER, Sound.LAND_SNOW, Sound.DIG_SNOW)
                .set(BlockComponents.CAN_BE_REPLACED, SnowLayerBlockHandlers.CAN_BE_REPLACED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.CAN_SURVIVE, SnowLayerBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.SNOW_LAYER_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(SnowLayerBlockHandlers.getResource(block.getState())))
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, SnowLayerBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.ON_RANDOM_TICK, SnowLayerBlockHandlers.ON_RANDOM_TICK);
        registry.configure(SOUL_FIRE)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.FIRE_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registry.configure(SOUL_SAND)
                .set(BlockComponents.ON_PLACE, BubbleColumnBlockHandlers.supportPlacement(registry))
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FULL_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> BubbleColumnBlockHandlers.updateAbove(block));
        configureTorch(registry, SOUL_TORCH);
        registry.configure(SPONGE)
                .set(BlockComponents.ON_PLACE, SpongeBlockHandlers.place(registry))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> SpongeBlockHandlers.absorb(block));
        configureWoodenButton(registry, SPRUCE_BUTTON);
        configureDoor(registry, SPRUCE_DOOR);
        configureFenceGate(registry, SPRUCE_FENCE_GATE);
        configureHangingSign(registry, SPRUCE_HANGING_SIGN);
        configureLeaves(registry, SPRUCE_LEAVES, VanillaBlockLoot.leaves(SPRUCE_LEAVES, SPRUCE_SAPLING, false));
        configureSlab(registry, SPRUCE_SLAB, SPRUCE_DOUBLE_SLAB);
        configureStairs(registry, SPRUCE_STAIRS);
        configureTrapdoor(registry, SPRUCE_TRAPDOOR);
        registry.configure(STONE)
                .set(BlockComponents.GET_LOOT,
                        (block, context) -> List.of(ItemStack.from(
                                context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0
                                        ? BlockTypes.STONE.getDefaultState()
                                        : BlockTypes.COBBLESTONE.getDefaultState())));
        configureSlab(registry, STONE_BRICK_SLAB, STONE_BRICK_DOUBLE_SLAB);
        configureStairs(registry, STONE_BRICK_STAIRS);
        configureStoneButton(registry, STONE_BUTTON);
        configureSlab(registry, STONE_SLAB, STONE_DOUBLE_SLAB);
        configureStairs(registry, STONE_STAIRS);
        configureUsable(registry, STONECUTTER, ContainerBlockHandlers.STONECUTTER);
        configureUsable(registry, STONECUTTER_BLOCK, ContainerBlockHandlers.STONECUTTER);
        configureBed(registry, STRAW_BED);
        configureSlab(registry, SULFUR_BRICK_SLAB, SULFUR_BRICK_DOUBLE_SLAB);
        configureStairs(registry, SULFUR_BRICK_STAIRS);
        configureSlab(registry, SULFUR_SLAB, SULFUR_DOUBLE_SLAB);
        configureStairs(registry, SULFUR_STAIRS);
        registry.configure(SWEET_BERRY_BUSH)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.SWEET_BERRY_BUSH_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        registry.configure(TALL_GRASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.tallGrass());
        configureTorch(registry, TORCH);
        configureUsable(registry, TRAPPED_CHEST, ContainerBlockHandlers.TRAPPED_CHEST);
        registry.configure(TRIP_WIRE)
                .set(BlockComponents.ON_PLACE, new TripwireBlockPlaceHandler())
                .set(BlockComponents.GET_LOOT, TripwireBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, TripwireBlockHandlers.GET_PICK_BLOCK)
                .set(BlockComponents.ON_ENTITY_INSIDE, TripwireBlockHandlers.ON_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE)
                .set(BlockComponents.ON_TICK, TripwireBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TripwireBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, TripwireBlockHandlers.ON_DESTROY);
        registry.configure(TRIPWIRE_HOOK)
                .set(BlockComponents.ON_PLACE, new TripwireHookPlaceHandler())
                .set(BlockComponents.ON_TICK, TripwireHookBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TripwireHookBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, TripwireHookBlockHandlers.ON_DESTROY);
        configureSlab(registry, TUFF_BRICK_SLAB, TUFF_BRICK_DOUBLE_SLAB);
        configureStairs(registry, TUFF_BRICK_STAIRS);
        configureSlab(registry, TUFF_SLAB, TUFF_DOUBLE_SLAB);
        configureStairs(registry, TUFF_STAIRS);
        configureTorch(registry, UNDERWATER_TORCH);
        configureShulkerBox(registry, UNDYED_SHULKER_BOX);
        configureTorch(registry, UNLIT_REDSTONE_TORCH);
        registry.configure(VINE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOnly(VINE));
        configureWoodenButton(registry, WARPED_BUTTON);
        configureDoor(registry, WARPED_DOOR);
        configureFenceGate(registry, WARPED_FENCE_GATE);
        configureHangingSign(registry, WARPED_HANGING_SIGN);
        configureSlab(registry, WARPED_SLAB, WARPED_DOUBLE_SLAB);
        configureStairs(registry, WARPED_STAIRS);
        configureTrapdoor(registry, WARPED_TRAPDOOR);
        configureLiquid(registry, WATER, LiquidTypes.WATER)
                .set(BlockComponents.ON_TICK, (block, random) -> LiquidBlockHandlers.tick(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block));
        configureDoor(registry, WAXED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_CUT_COPPER_SLAB, WAXED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, WAXED_CUT_COPPER_STAIRS);
        configureDoor(registry, WAXED_EXPOSED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_EXPOSED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_EXPOSED_CUT_COPPER_SLAB, WAXED_EXPOSED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, WAXED_EXPOSED_CUT_COPPER_STAIRS);
        configureDoor(registry, WAXED_OXIDIZED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_OXIDIZED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_OXIDIZED_CUT_COPPER_SLAB, WAXED_OXIDIZED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, WAXED_OXIDIZED_CUT_COPPER_STAIRS);
        configureDoor(registry, WAXED_WEATHERED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_WEATHERED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_WEATHERED_CUT_COPPER_SLAB, WAXED_WEATHERED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, WAXED_WEATHERED_CUT_COPPER_STAIRS);
        configureDoor(registry, WEATHERED_COPPER_DOOR);
        configureTrapdoor(registry, WEATHERED_COPPER_TRAPDOOR);
        configureSlab(registry, WEATHERED_CUT_COPPER_SLAB, WEATHERED_DOUBLE_CUT_COPPER_SLAB);
        configureStairs(registry, WEATHERED_CUT_COPPER_STAIRS);
        registry.configure(WEB)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.WEB_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.FULL_ENTITY_INSIDE_COLLISION_SHAPE);
        configureConcretePowder(registry, WHITE_CONCRETE_POWDER, WHITE_CONCRETE);
        configureShulkerBox(registry, WHITE_SHULKER_BOX);
        configureConcretePowder(registry, YELLOW_CONCRETE_POWDER, YELLOW_CONCRETE);
        configureLeaves(registry, YELLOW_POPLAR_LEAVES, VanillaBlockLoot.leaves(YELLOW_POPLAR_LEAVES, POPLAR_SAPLING, false));
        configureShulkerBox(registry, YELLOW_SHULKER_BOX);
    }

    private void configureAnvil(CloudBlockRegistry registry, BlockType type) {
        configureFalling(registry, type, Sound.RANDOM_ANVIL_LAND, Sound.RANDOM_ANVIL_BREAK, 2)
                .set(BlockComponents.ON_FALLING_LAND, FallingBlockHandlers.ANVIL_LAND)
                .set(BlockComponents.ON_PLACE, new AnvilPlaceHandler(registry))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ANVIL);
    }

    private void configureBed(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, BedBlockHandlers.BED)
                .set(BlockComponents.ON_PLACE, BedBlockHandlers.PLACE)
                .set(BlockComponents.ON_DESTROY, BedBlockHandlers.ON_DESTROY)
                .set(BlockComponents.GET_LOOT, BedBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, BedBlockHandlers.GET_PICK_BLOCK);
    }

    private void configureConcretePowder(CloudBlockRegistry registry, BlockType powderType, BlockType concreteType) {
        configureFalling(registry, powderType, Sound.LAND_SAND, Sound.DIG_SAND)
                .set(BlockComponents.ON_FALLING_LAND, FallingBlockHandlers.solidifyConcretePowder(concreteType));
    }

    private void configureDoor(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new DoorPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.GET_LOOT, DoorBlockHandlers.GET_LOOT)
                .set(BlockComponents.USE, DoorBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, DoorBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, DoorBlockHandlers.ON_DESTROY);
    }

    private ComponentBuilder configureFalling(
            CloudBlockRegistry registry, BlockType type, Sound landingSound, Sound breakSound) {
        return configureFalling(registry, type, landingSound, breakSound, 0);
    }

    private ComponentBuilder configureFalling(
            CloudBlockRegistry registry,
            BlockType type,
            Sound landingSound,
            Sound breakSound,
            float damagePerBlock) {
        return registry.configure(type)
                .set(BlockComponents.IS_FREE_TO_FALL, FallingBlockHandlers.IS_FREE_TO_FALL)
                .set(
                        BlockComponents.START_FALLING,
                        FallingBlockHandlers.startFalling(
                                landingSound, breakSound, damagePerBlock, MAXIMUM_FALL_DAMAGE))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FallingBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, FallingBlockHandlers.ON_TICK);
    }

    private void configureFenceGate(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FENCE_GATE_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.ON_PLACE, new FenceGatePlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, FenceGateBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FenceGateBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void configureHangingSign(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.OUTLINE_BLOCK_SUPPORT_SHAPE);
    }

    private void configureLeaves(CloudBlockRegistry registry, BlockType type, BlockLootHandler loot) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.EMPTY_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_LOOT, loot);
    }

    private ComponentBuilder configureLiquid(CloudBlockRegistry registry, BlockType blockType, LiquidType liquidType) {
        BlockRegistrationAccess.bindLiquidType(blockType, liquidType);
        return registry.configure(blockType);
    }

    private void configureOre(CloudBlockRegistry registry, BlockType type, OreLoot loot) {
        registry.configure(type)
                .set(BlockComponents.GET_LOOT, (block, context) -> loot.drops(context))
                .set(BlockComponents.GET_EXPERIENCE, (block, context) -> loot.experience(context));
    }

    private void configurePoweredRail(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, RailPlaceHandler.INSTANCE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PoweredRailBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REMOVE, RailBlockHandlers.ON_REMOVE);
    }

    private void configureShulkerBox(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.SHULKER_BOX_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_COLLISION_SHAPE, DefaultBlockHandlers.SHULKER_BOX_COLLISION_SHAPE)
                .set(BlockComponents.GET_OUTLINE_SHAPE, DefaultBlockHandlers.SHULKER_BOX_OUTLINE_SHAPE)
                .set(BlockComponents.ON_PLACE, new ShulkerBoxPlaceHandler(registry))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
    }

    private void configureSlab(CloudBlockRegistry registry, BlockType slabType, BlockType doubleSlabType) {
        registry.configure(slabType).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(doubleSlabType));
    }

    private void configureStairs(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type).set(BlockComponents.ON_PLACE, new StairsPlaceHandler(registry));
    }

    private void configureStoneButton(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.STONE_PRESS_TICKS)
                .set(BlockComponents.ON_PLACE, new ButtonPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ButtonBlockHandlers.USE)
                .set(BlockComponents.ON_TICK, ButtonBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ButtonBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void configureTorch(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new TorchPlaceHandler())
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TorchBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void configureTrapdoor(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new TrapdoorPlaceHandler(registry))
                .set(BlockComponents.CAN_BE_USED, TrapdoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, TrapdoorBlockHandlers.USE);
    }

    private void configureUsable(CloudBlockRegistry registry, BlockType type, UseBlockHandler handler) {
        registry.configure(type)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, handler);
    }

    private void configureWoodenButton(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.WOODEN_PRESS_TICKS)
                .set(BlockComponents.ON_PLACE, new ButtonPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ButtonBlockHandlers.USE)
                .set(BlockComponents.ON_TICK, ButtonBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ButtonBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }
}
