package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void suspendedFireConnectsToFlammableNeighbors() {
        BlockTestWorld world = world();
        world.setState(Direction.NORTH.relative(POSITION), BlockStates.OAK_PLANKS);
        world.setState(Direction.UP.relative(POSITION), BlockStates.OAK_LEAVES);
        world.setState(Direction.EAST.relative(POSITION), BlockStates.STONE);

        BlockState state = FireBlockHandlers.placementState(world.block(POSITION));

        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_NORTH));
        assertTrue(state.ensureTrait(BlockTraits.CONNECTION_UP));
        assertFalse(state.ensureTrait(BlockTraits.CONNECTION_EAST));
        assertFalse(state.ensureTrait(BlockTraits.CONNECTION_SOUTH));
        assertFalse(state.ensureTrait(BlockTraits.CONNECTION_WEST));
    }

    @Test
    void supportedFireHasNoSideConnections() {
        BlockTestWorld world = world();
        world.setState(Direction.DOWN.relative(POSITION), BlockStates.STONE);
        world.setState(Direction.NORTH.relative(POSITION), BlockStates.OAK_PLANKS);

        assertSame(BlockStates.FIRE, FireBlockHandlers.placementState(world.block(POSITION)));
    }

    @Test
    void waterloggedNeighborsDoNotAttractFlames() {
        BlockTestWorld world = world();
        Vector3i neighbor = Direction.NORTH.relative(POSITION);
        world.setState(neighbor, BlockStates.OAK_FENCE);
        world.setLiquid(neighbor, LiquidState.of(BlockStates.WATER));

        assertFalse(FireBlockHandlers.placementState(world.block(POSITION)).ensureTrait(BlockTraits.CONNECTION_NORTH));
    }

    @Test
    void neighborUpdatesRecomputeConnectionsWithoutResettingAge() {
        BlockTestWorld world = world();
        world.setState(POSITION, BlockStates.FIRE.withTrait(BlockTraits.AGE, 9).withTrait(BlockTraits.CONNECTION_NORTH, true));
        world.setState(Direction.EAST.relative(POSITION), BlockStates.OAK_PLANKS);

        FireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(Direction.NORTH.relative(POSITION)));

        assertEquals(9, world.state(POSITION).ensureTrait(BlockTraits.AGE));
        assertFalse(world.state(POSITION).ensureTrait(BlockTraits.CONNECTION_NORTH));
        assertTrue(world.state(POSITION).ensureTrait(BlockTraits.CONNECTION_EAST));
    }

    @Test
    void changingTheBaseToSoulSoilConvertsNormalFire() {
        BlockTestWorld world = world();
        world.setState(POSITION, BlockStates.FIRE);
        world.setState(Direction.DOWN.relative(POSITION), BlockStates.SOUL_SOIL);

        FireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(Direction.DOWN.relative(POSITION)));

        assertSame(BlockStates.SOUL_FIRE, world.state(POSITION));
    }

    @Test
    void removingTheLastSupportExtinguishesFire() {
        BlockTestWorld world = world();
        world.setState(POSITION, BlockStates.FIRE);

        FireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(Direction.DOWN.relative(POSITION)));

        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    private static BlockTestWorld world() {
        return new BlockTestWorld(BlockTestRegistry.get());
    }
}
