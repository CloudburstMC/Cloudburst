package org.cloudburstmc.server.block;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.BlockSupportShapeHandler;
import org.cloudburstmc.api.block.component.ShapeContextRequirement;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockSupportSnapshotTest {

    private static final Vector3i POSITION = Vector3i.from(4, 70, -6);

    @Test
    void proposedStateControlsSupportRatherThanTheExistingTarget() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Block stone = new CloudBlock(world.block(POSITION).getLevel(), POSITION,
                new BlockState[]{BlockStates.STONE, BlockStates.AIR});
        assertFalse(stone.getBlockSupportShape().isEmpty());
        assertTrue(stone.isFaceSturdy(Direction.UP, SupportType.FULL));

        world.setState(POSITION, BlockStates.STONE);
        Block air = new CloudBlock(stone.getLevel(), POSITION, new BlockState[]{BlockStates.AIR, BlockStates.AIR});
        assertTrue(air.getBlockSupportShape().isEmpty());
        assertFalse(air.isFaceSturdy(Direction.UP, SupportType.FULL));
        assertSame(BlockStates.STONE, world.state(POSITION));
    }

    @Test
    void capturedSupportRemainsStableUntilTheBlockIsRefreshed() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.STONE);
        Block captured = new CloudBlock(world.block(POSITION).getLevel(), POSITION,
                new BlockState[]{BlockStates.STONE, BlockStates.AIR});
        world.setState(POSITION, BlockStates.AIR);
        assertFalse(captured.getBlockSupportShape().isEmpty());
        assertTrue(captured.isFaceSturdy(Direction.UP, SupportType.FULL));
        assertSame(BlockStates.AIR, captured.refresh().getState());
    }

    @Test
    void contextualSupportUsesTheProposedStateAndLiveNeighborsWithoutCaching() {
        BlockRegistry registry = BlockTestRegistry.get();
        BlockTestWorld world = new BlockTestWorld(registry);
        Level surroundings = world.block(POSITION).getLevel();
        Vector3i neighbor = POSITION.add(1, 0, 0);
        BlockSupportShapeHandler handler = new BlockSupportShapeHandler() {
            @Override
            public VoxelShape execute(BlockState state, BlockShapeContext context) {
                assertSame(BlockStates.STONE, state);
                assertEquals(POSITION, context.position());
                return context.level().getBlockState(neighbor) == BlockStates.STONE
                        ? CloudVoxelShapes.block() : CloudVoxelShapes.empty();
            }

            @Override
            public ShapeContextRequirement contextRequirement() {
                return ShapeContextRequirement.LEVEL;
            }
        };

        BlockRegistry contextualRegistry = InterfaceProxy.create(BlockRegistry.class, (proxy, method, arguments) -> {
            if (method.getName().equals("requireComponent") && arguments[1] == BlockComponents.GET_BLOCK_SUPPORT_SHAPE) {
                return handler;
            }

            if (method.getName().equals("requireComponent") && arguments[1] == BlockComponents.CAN_SURVIVE) {
                return (SurviveBlockHandler) block -> block.isFaceSturdy(Direction.UP, SupportType.FULL);
            }

            return method.invoke(registry, arguments);
        });
        Server server = InterfaceProxy.create(Server.class, Map.of("getBlockRegistry", contextualRegistry));
        Level level = InterfaceProxy.create(Level.class, (proxy, method, arguments) ->
                method.getName().equals("getServer") ? server : method.invoke(surroundings, arguments));
        Block proposed = new CloudBlock(level, POSITION, new BlockState[]{BlockStates.STONE, BlockStates.AIR});

        assertTrue(proposed.getBlockSupportShape().isEmpty());
        assertFalse(proposed.isFaceSturdy(Direction.UP, SupportType.FULL));
        assertFalse(proposed.isSupported(level, POSITION));
        world.setState(neighbor, BlockStates.STONE);
        assertFalse(proposed.getBlockSupportShape().isEmpty());
        assertTrue(proposed.isFaceSturdy(Direction.UP, SupportType.FULL));
        assertTrue(proposed.isSupported(level, POSITION));
        world.setState(neighbor, BlockStates.AIR);
        assertFalse(proposed.isFaceSturdy(Direction.UP, SupportType.FULL));
        assertFalse(proposed.isSupported(level, POSITION));
        assertSame(BlockStates.AIR, world.state(POSITION));
    }
}
