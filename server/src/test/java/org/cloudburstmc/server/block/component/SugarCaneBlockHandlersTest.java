package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SugarCaneBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final Vector3i BELOW = POSITION.add(0, -1, 0);
    private static final Vector3i WATER = BELOW.add(1, 0, 0);
    private static final BlockState CANE = BlockTypes.REEDS.getDefaultState();
    private static final BlockState MATURE = CANE.withTrait(BlockTraits.AGE, 15);

    @Test
    void heldSugarCaneResolvesToReedsWithoutAStoredBlockState() {
        CloudItemRegistry items = CloudItemRegistry.get();
        ItemStack held = ItemStack.from(ItemTypes.SUGAR_CANE);

        assertFalse(held.has(ItemDataComponents.BLOCK_STATE));

        Optional<?> placement = assertInstanceOf(Optional.class, items.requireComponent(held.getType(), ItemBehaviors.GET_BLOCK).execute(held));

        assertSame(CANE, placement.orElseThrow());
        assertSame(ItemTypes.SUGAR_CANE, BlockTypes.REEDS.asItem().orElseThrow());
        assertSame(ItemTypes.SUGAR_CANE, items.get(BlockTypes.REEDS.getId()).orElseThrow());

        BlockTestWorld world = world();
        world.setState(POSITION, assertInstanceOf(BlockState.class, placement.orElseThrow()));

        assertTrue(REGISTRY.requireComponent(BlockTypes.REEDS, BlockComponents.CAN_SURVIVE).execute(world.block(POSITION)));
    }

    @Test
    void supportsVerifiedGroundTypesButNotFarmlandOrStone() {
        BlockTestWorld world = world();
        for (BlockType support : List.of(BlockTypes.DIRT, BlockTypes.COARSE_DIRT, BlockTypes.DIRT_WITH_ROOTS,
                BlockTypes.MUD, BlockTypes.MUDDY_MANGROVE_ROOTS, BlockTypes.MOSS_BLOCK, BlockTypes.PALE_MOSS_BLOCK,
                BlockTypes.GRASS_BLOCK, BlockTypes.PODZOL, BlockTypes.MYCELIUM,
                BlockTypes.SAND, BlockTypes.RED_SAND, BlockTypes.SUSPICIOUS_SAND)) {
            world.setState(BELOW, support.getDefaultState());

            assertTrue(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)), support.getId().toString());
        }

        for (BlockType support : List.of(BlockTypes.AIR, BlockTypes.STONE, BlockTypes.FARMLAND, BlockTypes.CLAY)) {
            world.setState(BELOW, support.getDefaultState());

            assertFalse(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)), support.getId().toString());
        }
    }

    @Test
    void acceptsFlowingAndContainedWaterAndFrostedIceAtGroundHeight() {
        BlockTestWorld world = world();
        for (BlockState adjacent : List.of(BlockStates.WATER,
                BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 3), BlockTypes.FROSTED_ICE.getDefaultState())) {
            world.setState(WATER, adjacent);

            assertTrue(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        world.setState(WATER, BlockTypes.MANGROVE_ROOTS.getDefaultState());
        world.setLiquid(WATER, LiquidState.of(BlockStates.WATER));

        assertTrue(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
    }

    @Test
    void rejectsWrongFluidsHeightsDiagonalsAndUnloadedWater() {
        BlockTestWorld world = world();
        for (BlockState adjacent : List.of(BlockStates.AIR, BlockStates.LAVA, BlockTypes.ICE.getDefaultState())) {
            world.setState(WATER, adjacent);

            assertFalse(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        }

        world.setState(WATER, BlockStates.AIR);
        world.setState(POSITION.add(1, 0, 0), BlockStates.WATER);
        world.setState(BELOW.add(1, 0, 1), BlockStates.WATER);

        assertFalse(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

        world.setState(WATER, BlockStates.WATER);
        world.unload(WATER);

        assertFalse(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
    }

    @Test
    void upperSegmentsSurviveOnCaneWithoutAPlacementHeightLimit() {
        BlockTestWorld world = world();
        world.setState(WATER, BlockStates.AIR);
        world.setState(BELOW, CANE);

        assertTrue(SugarCaneBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
    }

    @Test
    void losingSupportSchedulesDestructionAndRechecksBeforeBreaking() {
        BlockTestWorld world = world();
        world.setState(BELOW, BlockStates.AIR);

        SugarCaneBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(BELOW));

        assertEquals(List.of(1), world.getScheduledDelays());
        assertSame(CANE, world.state(POSITION));

        world.setState(BELOW, BlockTypes.SAND.getDefaultState());
        SugarCaneBlockHandlers.ON_TICK.execute(world.block(POSITION), null);

        assertEquals(0, world.getBreakCount());

        world.setState(BELOW, BlockStates.AIR);
        SugarCaneBlockHandlers.ON_TICK.execute(world.block(POSITION), null);

        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(1, world.getBreakCount());
    }

    @Test
    void growsAfterSixteenRandomTicksAndUsesBlockGrowEvent() {
        BlockTestWorld world = world();
        for (int tick = 0; tick < 15; tick++) {
            randomTick(world, POSITION);
        }

        assertSame(MATURE, world.state(POSITION));
        assertSame(BlockStates.AIR, world.state(POSITION.add(0, 1, 0)));
        assertTrue(world.getEvents(BlockGrowEvent.class).isEmpty());

        randomTick(world, POSITION);

        assertSame(CANE, world.state(POSITION));
        assertSame(CANE, world.state(POSITION.add(0, 1, 0)));
        List<BlockGrowEvent> events = world.getEvents(BlockGrowEvent.class);
        assertEquals(1, events.size());
        assertEquals(POSITION.add(0, 1, 0), events.getFirst().getBlock().getPosition());
        assertSame(CANE, events.getFirst().getNewState());
    }

    @Test
    void cancellationPreventsTheNewSegmentButStillResetsAge() {
        BlockTestWorld world = world();
        world.setState(POSITION, MATURE);
        world.onEvent(BlockGrowEvent.class, event -> event.setCancelled(true));

        randomTick(world, POSITION);

        assertSame(CANE, world.state(POSITION));
        assertSame(BlockStates.AIR, world.state(POSITION.add(0, 1, 0)));
    }

    @Test
    void growthRespectsTheEventReplacement() {
        BlockTestWorld world = world();
        world.setState(POSITION, MATURE);
        world.onEvent(BlockGrowEvent.class, event -> event.setNewState(BlockStates.STONE));

        randomTick(world, POSITION);

        assertSame(BlockStates.STONE, world.state(POSITION.add(0, 1, 0)));
        assertSame(CANE, world.state(POSITION));
    }

    @Test
    void growthDoesNotOverwriteSourceOrTargetChangesMadeByListeners() {
        BlockTestWorld sourceChanged = world();
        sourceChanged.setState(POSITION, MATURE);
        sourceChanged.onEvent(BlockGrowEvent.class, event -> sourceChanged.setState(POSITION, BlockStates.STONE));

        randomTick(sourceChanged, POSITION);

        assertSame(BlockStates.STONE, sourceChanged.state(POSITION));
        assertSame(BlockStates.AIR, sourceChanged.state(POSITION.add(0, 1, 0)));

        BlockTestWorld targetChanged = world();
        targetChanged.setState(POSITION, MATURE);
        targetChanged.onEvent(BlockGrowEvent.class, event -> targetChanged.setState(POSITION.add(0, 1, 0), BlockStates.STONE));

        randomTick(targetChanged, POSITION);

        assertSame(BlockStates.STONE, targetChanged.state(POSITION.add(0, 1, 0)));
        assertSame(CANE, targetChanged.state(POSITION));
    }

    @Test
    void naturalGrowthStopsAtThreeBlocksWithoutAdvancingAge() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, 1, 0), MATURE);

        randomTick(world, POSITION.add(0, 1, 0));

        assertSame(CANE, world.state(POSITION.add(0, 1, 0)));
        assertSame(CANE, world.state(POSITION.add(0, 2, 0)));
        assertEquals(1, world.getEvents(BlockGrowEvent.class).size());

        world.setState(POSITION.add(0, 2, 0), MATURE);

        randomTick(world, POSITION.add(0, 2, 0));

        assertSame(MATURE, world.state(POSITION.add(0, 2, 0)));
        assertSame(BlockStates.AIR, world.state(POSITION.add(0, 3, 0)));
        assertEquals(1, world.getEvents(BlockGrowEvent.class).size());
    }

    @Test
    void naturalGrowthRequiresLoadedAirAboveAndStaysInsideBuildHeight() {
        for (BlockState obstruction : List.of(BlockStates.STONE, BlockStates.WATER, BlockTypes.SHORT_GRASS.getDefaultState())) {
            BlockTestWorld world = world();
            world.setState(POSITION, MATURE);
            world.setState(POSITION.add(0, 1, 0), obstruction);

            randomTick(world, POSITION);

            assertSame(MATURE, world.state(POSITION));
            assertSame(obstruction, world.state(POSITION.add(0, 1, 0)));
            assertTrue(world.getEvents(BlockGrowEvent.class).isEmpty());
        }

        BlockTestWorld unloaded = world();
        unloaded.setState(POSITION, MATURE);
        unloaded.unload(POSITION.add(0, 1, 0));

        randomTick(unloaded, POSITION);

        assertSame(MATURE, unloaded.state(POSITION));

        BlockTestWorld ceiling = world();
        Vector3i top = Vector3i.from(0, 319, 0);
        ceiling.setState(top, MATURE);

        randomTick(ceiling, top);

        assertSame(MATURE, ceiling.state(top));
        assertSame(BlockStates.AIR, ceiling.state(top.add(0, 1, 0)));
    }

    @Test
    void boneMealProposesTheMissingSegmentsWithoutChangingTheWorld() {
        BlockTestWorld world = world();
        FertilizationResult result = SugarCaneBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(1));

        assertEquals(List.of(POSITION.add(0, 1, 0), POSITION.add(0, 2, 0)),
                result.changes().stream().map(change -> change.block().getPosition()).toList());
        assertTrue(result.changes().stream().allMatch(change -> change.newState() == CANE));
        assertTrue(result.drops().isEmpty());
        assertSame(BlockStates.AIR, world.state(POSITION.add(0, 1, 0)));

        world.setState(POSITION.add(0, 1, 0), CANE);
        FertilizationResult fromUpper = SugarCaneBlockHandlers.FERTILIZE.execute(world.block(POSITION.add(0, 1, 0)), null, new Random(1));

        assertEquals(1, fromUpper.changes().size());
        assertEquals(POSITION.add(0, 2, 0), fromUpper.changes().getFirst().block().getPosition());

        world.setState(POSITION.add(0, 2, 0), CANE);

        assertFalse(SugarCaneBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(1)).succeeded());
    }

    @Test
    void boneMealStopsAtObstructionsAndRejectsUnsupportedCane() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);

        assertFalse(SugarCaneBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(1)).succeeded());

        world.setState(POSITION.add(0, 1, 0), BlockStates.AIR);
        world.unload(POSITION.add(0, 2, 0));
        FertilizationResult partial = SugarCaneBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(1));

        assertEquals(1, partial.changes().size());

        world.setState(WATER, BlockStates.AIR);

        assertFalse(SugarCaneBlockHandlers.FERTILIZE.execute(world.block(POSITION), null, new Random(1)).succeeded());
    }

    @Test
    void dropsAndPicksSugarCaneInsteadOfAnInternalBlockItem() {
        BlockTestWorld world = world();
        BlockLootContext context = BlockLootContext.empty(new Random(1));

        assertSame(ItemTypes.SUGAR_CANE, REGISTRY.requireComponent(BlockTypes.REEDS, BlockComponents.GET_LOOT)
                .execute(world.block(POSITION), context).getFirst().getType());
        assertSame(ItemTypes.SUGAR_CANE, REGISTRY.requireComponent(BlockTypes.REEDS, BlockComponents.GET_PICK_BLOCK)
                .execute(world.block(POSITION)).getType());
    }

    private static void randomTick(BlockTestWorld world, Vector3i position) {
        REGISTRY.requireComponent(BlockTypes.REEDS, BlockComponents.ON_RANDOM_TICK).execute(world.block(position), new Random(1));
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, CANE);
        world.setState(BELOW, BlockTypes.SAND.getDefaultState());
        world.setState(WATER, BlockStates.WATER);
        return world;
    }
}
