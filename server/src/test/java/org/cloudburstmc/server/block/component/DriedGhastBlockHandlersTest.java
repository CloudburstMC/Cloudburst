package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriedGhastBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState DRIED_GHAST = BlockTypes.DRIED_GHAST.getDefaultState();

    @Test
    void randomTicksScheduleOneHydrationTimerWithoutRestartingIt() {
        BlockTestWorld world = world(0, BlockStates.WATER);
        Block block = world.block(POSITION);

        assertTrue(REGISTRY.requireComponent(BlockTypes.DRIED_GHAST, BlockComponents.CAN_RANDOM_TICK));
        REGISTRY.requireComponent(BlockTypes.DRIED_GHAST, BlockComponents.ON_RANDOM_TICK).execute(block, null);
        DriedGhastBlockHandlers.ON_RANDOM_TICK.execute(block, null);

        assertEquals(List.of(5000), world.getScheduledDelays());
    }

    @Test
    void dryUnhydratedBlocksDoNotScheduleButPartiallyHydratedBlocksDo() {
        BlockTestWorld world = world(0, BlockStates.AIR);

        DriedGhastBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), null);
        assertTrue(world.getScheduledDelays().isEmpty());

        world.setState(POSITION, DRIED_GHAST.withTrait(BlockTraits.REHYDRATION_LEVEL, 2));
        DriedGhastBlockHandlers.ON_RANDOM_TICK.execute(world.block(POSITION), null);
        assertEquals(List.of(5000), world.getScheduledDelays());
    }

    @Test
    void sourceWaterHydratesThroughEveryStageWithoutLosingWaterOrFacing() {
        BlockTestWorld world = world(0, BlockStates.WATER);

        for (int hydration = 1; hydration <= 3; hydration++) {
            REGISTRY.requireComponent(BlockTypes.DRIED_GHAST, BlockComponents.ON_TICK).execute(world.block(POSITION), null);

            assertEquals(hydration, world.state(POSITION).ensureTrait(BlockTraits.REHYDRATION_LEVEL));
            assertEquals(DRIED_GHAST.ensureTrait(BlockTraits.CARDINAL_DIRECTION),
                    world.state(POSITION).ensureTrait(BlockTraits.CARDINAL_DIRECTION));
            assertTrue(world.block(POSITION).getLiquid().isSource());
        }

        assertEquals(3, world.getEvents(BlockGrowEvent.class).size());
        assertEquals(List.of(SoundTypes.BLOCK_DRIED_GHAST_STATE_CHANGE,
                SoundTypes.BLOCK_DRIED_GHAST_STATE_CHANGE, SoundTypes.BLOCK_DRIED_GHAST_STATE_CHANGE), world.getSounds());
    }

    @Test
    void flowingWaterLavaAndDryAirDoNotHydrate() {
        for (BlockState liquid : List.of(BlockStates.AIR, BlockStates.LAVA,
                BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 1),
                BlockStates.FLOWING_WATER.withTrait(BlockTraits.LIQUID_DEPTH, 8))) {
            BlockTestWorld world = world(0, liquid);

            DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);

            assertSame(DRIED_GHAST, world.state(POSITION));
            assertTrue(world.getEvents(BlockGrowEvent.class).isEmpty());
            assertTrue(world.getSounds().isEmpty());
        }
    }

    @Test
    void removingWaterDehydratesOneStagePerScheduledTick() {
        BlockTestWorld world = world(3, BlockStates.AIR);

        for (int hydration = 2; hydration >= 0; hydration--) {
            DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
            assertEquals(hydration, world.state(POSITION).ensureTrait(BlockTraits.REHYDRATION_LEVEL));
        }

        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(DRIED_GHAST, world.state(POSITION));
        assertTrue(world.getEvents(BlockGrowEvent.class).isEmpty());
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void hydrationHonorsCancellationAndListenerReplacement() {
        BlockTestWorld world = world(0, BlockStates.WATER);
        world.onEvent(BlockGrowEvent.class, event -> event.setCancelled(true));

        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(DRIED_GHAST, world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());

        world.onEvent(BlockGrowEvent.class, event -> event.setNewState(BlockStates.STONE));
        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(BlockStates.STONE, world.state(POSITION));
    }

    @Test
    void hydrationDoesNotOverwriteListenerChangesOrContinueAfterWaterRemoval() {
        BlockTestWorld world = world(0, BlockStates.WATER);
        world.onEvent(BlockGrowEvent.class, event -> world.setState(POSITION, BlockStates.STONE));

        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());

        world.setState(POSITION, DRIED_GHAST);
        world.onEvent(BlockGrowEvent.class, event -> world.setLiquid(POSITION, LiquidState.empty()));
        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(DRIED_GHAST, world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void hatchingCanBeCancelledOrReplacedWithoutSpawning() {
        BlockTestWorld world = world(3, BlockStates.WATER);
        BlockState hydrated = world.state(POSITION);
        world.onEvent(BlockFadeEvent.class, event -> {
            assertSame(BlockStates.WATER, event.getNewState());
            event.setCancelled(true);
        });

        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(hydrated, world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());

        world.onEvent(BlockFadeEvent.class, event -> event.setNewState(BlockStates.STONE));
        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);
        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void hatchingDoesNotOverwriteChangesMadeByListeners() {
        BlockTestWorld world = world(3, BlockStates.WATER);
        world.onEvent(BlockFadeEvent.class, event -> world.setState(POSITION, BlockStates.STONE));

        DriedGhastBlockHandlers.ON_TICK.execute(world.block(POSITION), null);

        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void waterEntrySoundDoesNotReplayForUnchangedWater() {
        BlockTestWorld world = world(0, BlockStates.WATER);

        DriedGhastBlockHandlers.ON_LIQUID_CHANGED.execute(world.block(POSITION), LiquidState.empty());
        DriedGhastBlockHandlers.ON_LIQUID_CHANGED.execute(world.block(POSITION), LiquidState.of(BlockStates.WATER));
        assertEquals(List.of(SoundTypes.BLOCK_DRIED_GHAST_PLACE_IN_WATER), world.getSounds());

        world.setLiquid(POSITION, LiquidState.empty());
        DriedGhastBlockHandlers.ON_LIQUID_CHANGED.execute(world.block(POSITION), LiquidState.of(BlockStates.WATER));
        assertEquals(1, world.getSounds().size());
    }

    @Test
    void placingInWaterPreservesSourceWaterAndUsesTheWetPlacementSound() {
        BlockTestWorld world = world(0, BlockStates.AIR);
        world.setState(POSITION, BlockStates.WATER);
        Block block = world.block(POSITION);

        assertTrue(block.getLevel().setBlockState(POSITION, DRIED_GHAST));
        Block placed = world.block(POSITION);
        assertSame(BlockStates.WATER, placed.getState(BlockLayer.SECONDARY));

        DriedGhastBlockHandlers.ON_ADDED.execute(placed);
        assertEquals(List.of(SoundTypes.BLOCK_DRIED_GHAST_PLACE_IN_WATER), world.getSounds());
    }

    private static BlockTestWorld world(int hydration, BlockState liquid) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, DRIED_GHAST.withTrait(BlockTraits.REHYDRATION_LEVEL, hydration));
        world.setLiquid(POSITION, LiquidState.of(liquid));
        return world;
    }
}
