package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallingBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void eachPlacementSchedulesGravityWithoutANeighbourUpdate() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i next = POSITION.add(1, 0, 0);
        Vector3i last = POSITION.add(2, 0, 0);

        for (Vector3i position : List.of(POSITION, next, last)) {
            world.setState(position, BlockStates.SAND);
            Block added = world.block(position);
            added.requireComponent(BlockComponents.ON_ADDED).execute(added);
        }

        assertEquals(Map.of(POSITION, 2, next, 2, last, 2), world.getScheduledUpdates());
    }

    @Test
    void aNeighbourChangeSchedulesGravityBeforeCheckingSupport() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i below = POSITION.sub(0, 1, 0);
        world.setState(POSITION, BlockStates.SAND);
        world.setState(below, BlockStates.STONE);

        Block sand = world.block(POSITION);
        sand.requireComponent(BlockComponents.ON_NEIGHBOUR_CHANGED)
                .execute(sand, world.block(POSITION.add(1, 0, 0)));

        assertEquals(Map.of(POSITION, 1), world.getScheduledUpdates());
        assertFalse(sand.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(sand));

        world.setState(below, BlockStates.AIR);
        assertTrue(sand.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(sand));
    }

    @Test
    void fallDelaysDependOnTheTriggerRatherThanTheBlockType() {
        assertFallDelays(BlockTypes.GRAVEL);
        assertFallDelays(BlockTypes.DRAGON_EGG);
    }

    private static void assertFallDelays(BlockType type) {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, type.getDefaultState());
        Block block = world.block(POSITION);

        block.requireComponent(BlockComponents.ON_ADDED).execute(block);
        block.requireComponent(BlockComponents.ON_NEIGHBOUR_CHANGED)
                .execute(block, world.block(POSITION.sub(0, 1, 0)));

        assertEquals(List.of(2, 1), world.getScheduledDelays(), type.toString());
    }
}
