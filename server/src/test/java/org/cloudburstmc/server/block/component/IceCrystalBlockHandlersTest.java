package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IceCrystalBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 70, 0);

    @Test
    void placementUsesClickedFaceAndRequiresTheOppositeSupportFace() {
        for (Direction facing : Direction.values()) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            BlockState state = IceCrystalBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                    BlockTypes.ICE_CRYSTAL.getDefaultState(), world.block(POSITION), null, facing, Vector3f.ZERO);

            assertNotNull(state);
            world.setState(POSITION, state);
            assertEquals(facing, state.ensureTrait(BlockTraits.BLOCK_FACE));
            assertFalse(IceCrystalBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

            Vector3i support = facing.getOpposite().relative(POSITION);
            world.setState(support, BlockStates.STONE);
            assertTrue(IceCrystalBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));

            world.setState(support, BlockStates.AIR);
            IceCrystalBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(support));
            assertSame(BlockStates.AIR, world.state(POSITION));
        }
    }

    @Test
    void unrelatedNeighborChangesDoNotBreakAnUnsupportedCrystal() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        BlockState state = BlockTypes.ICE_CRYSTAL.getDefaultState().withTrait(BlockTraits.BLOCK_FACE, Direction.UP);
        world.setState(POSITION, state);

        IceCrystalBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(1, 0, 0)));

        assertSame(state, world.state(POSITION));
        assertEquals(0, world.getBreakCount());
    }
}
