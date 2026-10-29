package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ChorusBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void placementConnectsToFlowersAndPlantsOnEveryFace() {
        BlockTestWorld world = world();
        for (Direction direction : Direction.values()) {
            world.setState(direction.relative(POSITION), direction == Direction.UP ? BlockStates.CHORUS_FLOWER : BlockStates.CHORUS_PLANT);
        }

        BlockState state = ChorusBlockHandlers.PLANT_PLACEMENT.execute(BlockStates.CHORUS_PLANT,
                world.block(POSITION), null, Direction.UP, Vector3f.ZERO);

        assertNotNull(state);
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_DOWN));
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_UP));
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_NORTH));
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_SOUTH));
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_EAST));
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_WEST));
    }

    @Test
    void endStoneOnlyConnectsFromBelow() {
        BlockState connected = ChorusBlockHandlers.resolveConnections(BlockStates.CHORUS_PLANT, direction -> BlockStates.END_STONE);

        assertTrue(connected.ensureTrait(BlockTraits.CONNECTION_DOWN));
        assertFalse(connected.ensureTrait(BlockTraits.CONNECTION_UP));
        assertFalse(connected.ensureTrait(BlockTraits.CONNECTION_NORTH));
        assertFalse(connected.ensureTrait(BlockTraits.CONNECTION_SOUTH));
        assertFalse(connected.ensureTrait(BlockTraits.CONNECTION_EAST));
        assertFalse(connected.ensureTrait(BlockTraits.CONNECTION_WEST));
    }

    @Test
    void neighborChangesRemoveStaleConnections() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, -1, 0), BlockStates.END_STONE);
        world.setState(POSITION, BlockStates.CHORUS_PLANT.withTrait(BlockTraits.CONNECTION_EAST, true));

        ChorusBlockHandlers.PLANT_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(1, 0, 0)));

        assertFalse(world.state(POSITION).ensureTrait(BlockTraits.CONNECTION_EAST));
        assertTrue(world.state(POSITION).ensureTrait(BlockTraits.CONNECTION_DOWN));
    }

    @Test
    void unsupportedPlantsBreakOnTheScheduledTick() {
        BlockTestWorld world = world();
        world.setState(POSITION, BlockStates.CHORUS_PLANT);

        ChorusBlockHandlers.PLANT_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));

        assertEquals(1, world.getScheduledUpdates().get(POSITION));
        assertSame(BlockStates.CHORUS_PLANT, world.state(POSITION));

        ChorusBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));

        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void growingFlowersLeaveAConnectedStem() {
        BlockTestWorld world = world();
        world.setState(POSITION, BlockStates.CHORUS_FLOWER);
        world.setState(POSITION.add(0, -1, 0), BlockStates.END_STONE);

        ChorusBlockHandlers.FLOWER_RANDOM_TICK.execute(world.block(POSITION), new Random(0));

        assertSame(BlockTypes.CHORUS_PLANT, world.state(POSITION).getType());
        assertTrue(world.state(POSITION).ensureTrait(BlockTraits.CONNECTION_DOWN));
        assertTrue(world.state(POSITION).ensureTrait(BlockTraits.CONNECTION_UP));
    }

    private static BlockTestWorld world() {
        return new BlockTestWorld(BlockTestRegistry.get());
    }
}
