package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DryVegetationTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final Vector3i BELOW = POSITION.sub(0, 1, 0);

    @Test
    void dryPlantsSharePlacementAndSupportLossRules() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        for (BlockType type : List.of(BlockTypes.DEADBUSH, BlockTypes.SHORT_DRY_GRASS, BlockTypes.TALL_DRY_GRASS)) {
            world.setState(POSITION, type.getDefaultState());
            for (BlockType support : List.of(BlockTypes.DIRT, BlockTypes.RED_SAND, BlockTypes.WHITE_TERRACOTTA)) {
                world.setState(BELOW, support.getDefaultState());
                Block plant = world.block(POSITION);
                assertTrue(plant.getComponents().require(BlockComponents.CAN_SURVIVE).execute(plant));
            }

            world.setState(BELOW, BlockStates.STONE);
            Block plant = world.block(POSITION);
            assertFalse(plant.getComponents().require(BlockComponents.CAN_SURVIVE).execute(plant));
            plant.getComponents().require(BlockComponents.ON_NEIGHBOUR_CHANGED).execute(plant, world.block(BELOW));
            assertSame(BlockStates.AIR, world.state(POSITION));
        }
    }

    @Test
    void deadBushDropsItselfOnlyWithShearsAndOtherwiseRollsForSticks() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockTypes.DEADBUSH.getDefaultState());
        Block bush = world.block(POSITION);

        assertEquals(List.of(ItemStack.from(bush.getState())), loot(bush, ItemStack.from(ItemTypes.SHEARS), 0));
        assertTrue(loot(bush, ItemStack.EMPTY, 0).isEmpty());
        assertEquals(List.of(ItemStack.from(ItemTypes.STICK)), loot(bush, ItemStack.EMPTY, 1));
        assertEquals(List.of(ItemStack.from(ItemTypes.STICK, 2)), loot(bush, silkTouchTool(), 2));
    }

    @Test
    void dryGrassDropsOnlyWithShearsOrSilkTouch() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        for (BlockType type : List.of(BlockTypes.SHORT_DRY_GRASS, BlockTypes.TALL_DRY_GRASS)) {
            world.setState(POSITION, type.getDefaultState());
            Block grass = world.block(POSITION);
            List<ItemStack> expected = List.of(ItemStack.from(grass.getState()));

            assertTrue(loot(grass, ItemStack.EMPTY, 2).isEmpty());
            assertEquals(expected, loot(grass, ItemStack.from(ItemTypes.SHEARS), 0));
            assertEquals(expected, loot(grass, silkTouchTool(), 0));
        }
    }

    @Test
    void tallDryGrassSpreadsOnlyToEmptyGround() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i east = POSITION.add(1, 0, 0);
        Vector3i west = POSITION.sub(1, 0, 0);
        world.setState(POSITION, BlockTypes.SHORT_DRY_GRASS.getDefaultState());
        world.setState(east.sub(0, 1, 0), BlockTypes.SAND.getDefaultState());
        world.setState(west.sub(0, 1, 0), BlockTypes.SAND.getDefaultState());
        world.setState(west, BlockTypes.DEADBUSH.getDefaultState());

        FertilizationResult growth = fertilize(world.block(POSITION));
        assertSame(BlockTypes.TALL_DRY_GRASS, growth.changes().getFirst().newState().getType());

        world.setState(POSITION, growth.changes().getFirst().newState());
        FertilizationResult spread = fertilize(world.block(POSITION));
        assertEquals(1, spread.changes().size());
        assertEquals(east, spread.changes().getFirst().block().getPosition());
        assertSame(BlockTypes.SHORT_DRY_GRASS, spread.changes().getFirst().newState().getType());

        world.setState(east, BlockTypes.WATER.getDefaultState());
        assertFalse(fertilize(world.block(POSITION)).succeeded());
    }

    private static FertilizationResult fertilize(Block block) {
        return block.getComponents().require(BlockComponents.FERTILIZE).execute(block, null, new Random(0));
    }

    private static List<ItemStack> loot(Block block, ItemStack tool, int roll) {
        Random random = new Random() {
            @Override
            public int nextInt(int bound) {
                assertEquals(3, bound);
                return roll;
            }
        };
        return block.getComponents().require(BlockComponents.GET_LOOT)
                .execute(block, new BlockLootContext(tool, null, random));
    }

    private static ItemStack silkTouchTool() {
        return ItemStack.builder().itemType(ItemTypes.DIAMOND_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();
    }
}
