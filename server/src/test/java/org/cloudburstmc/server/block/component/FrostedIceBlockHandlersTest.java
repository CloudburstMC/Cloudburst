package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FrostedIceBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState FROST = BlockTypes.FROSTED_ICE.getDefaultState().withTrait(BlockTraits.AGE, 0);

    @Test
    void placementSchedulesTheFirstTickBetweenSixtyAndOneHundredTwentyTicks() {
        BlockTestWorld world = world(FROST);
        FrostedIceBlockHandlers.ON_ADDED.execute(world.block(POSITION));

        assertEquals(1, world.getScheduledDelays().size());
        int delay = world.getScheduledDelays().getFirst();
        assertTrue(delay >= 60 && delay <= 120);
    }

    @Test
    void agingIceDoesNotMeltItsSparseNeighbors() {
        BlockTestWorld world = world(FROST);
        Vector3i neighbor = POSITION.add(1, 0, 0);
        world.setState(neighbor, FROST);
        world.enableNeighborUpdates(BlockTypes.FROSTED_ICE);

        FrostedIceBlockHandlers.tick(world.block(POSITION), new Random(0), 15, false);

        assertEquals(1, world.state(POSITION).ensureTrait(BlockTraits.AGE));
        assertSame(FROST, world.state(neighbor));
        assertEquals(1, world.getEvents(BlockFadeEvent.class).size());
    }

    @Test
    void warmIsolatedIceAgesThroughEveryStageBeforeMelting() {
        BlockTestWorld world = world(FROST);
        for (int age = 1; age <= 3; age++) {
            FrostedIceBlockHandlers.tick(world.block(POSITION), new Random(0), 15, false);
            assertEquals(age, world.state(POSITION).ensureTrait(BlockTraits.AGE));
        }

        assertEquals(3, world.getEvents(BlockFadeEvent.class).size());
        assertTrue(world.getScheduledDelays().stream().allMatch(delay -> delay >= 20 && delay <= 40));

        FrostedIceBlockHandlers.tick(world.block(POSITION), new Random(0), 15, false);
        assertSame(BlockStates.WATER, world.state(POSITION));
        assertEquals(4, world.getEvents(BlockFadeEvent.class).size());
    }

    @Test
    void darknessAndDenseNeighborsCanDelayAging() {
        BlockTestWorld dark = world(FROST);
        FrostedIceBlockHandlers.tick(dark.block(POSITION), new Random(0), 0, false);
        assertSame(FROST, dark.state(POSITION));
        assertEquals(1, dark.getScheduledDelays().size());

        BlockTestWorld dense = world(FROST);
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            dense.setState(direction.relative(POSITION), FROST);
        }

        Random failedRoll = new Random(0) {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };
        FrostedIceBlockHandlers.tick(dense.block(POSITION), failedRoll, 15, false);
        assertSame(FROST, dense.state(POSITION));
        assertTrue(dense.getEvents(BlockFadeEvent.class).isEmpty());
    }

    @Test
    void cancellingAgingOrTheFinalMeltKeepsIceAndSchedulesAnotherTick() {
        for (int age : List.of(0, 3)) {
            BlockState state = FROST.withTrait(BlockTraits.AGE, age);
            BlockTestWorld world = world(state);
            Vector3i neighbor = POSITION.add(1, 0, 0);
            world.setState(neighbor, FROST);
            world.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));

            FrostedIceBlockHandlers.tick(world.block(POSITION), new Random(0), 15, false);

            assertSame(state, world.state(POSITION));
            assertSame(FROST, world.state(neighbor));
            assertEquals(1, world.getScheduledDelays().size());
        }
    }

    @Test
    void finalMeltingAlsoAgesSurvivingNeighbors() {
        BlockTestWorld world = world(FROST.withTrait(BlockTraits.AGE, 3));
        Vector3i neighbor = POSITION.add(1, 0, 0);
        world.setState(neighbor, FROST);
        world.setState(neighbor.add(1, 0, 0), FROST);
        world.setState(neighbor.add(0, 0, 1), FROST);

        FrostedIceBlockHandlers.tick(world.block(POSITION), new Random(0), 15, false);

        assertSame(BlockStates.WATER, world.state(POSITION));
        assertEquals(1, world.state(neighbor).ensureTrait(BlockTraits.AGE));
        assertTrue(world.getScheduledUpdates().containsKey(neighbor));
    }

    @Test
    void removingFrostedIceMeltsSparseNeighborsButRespectsCancellation() {
        for (boolean cancel : List.of(false, true)) {
            BlockTestWorld world = world(FROST);
            Block removed = world.block(POSITION);
            Vector3i neighbor = POSITION.add(1, 0, 0);
            world.setState(neighbor, FROST);
            world.setState(POSITION, BlockStates.AIR);
            world.onEvent(BlockFadeEvent.class, event -> event.setCancelled(cancel));

            FrostedIceBlockHandlers.removed(removed, false);

            assertSame(cancel ? FROST : BlockStates.WATER, world.state(neighbor));
        }
    }

    private static BlockTestWorld world(BlockState state) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, state);
        return world;
    }
}
