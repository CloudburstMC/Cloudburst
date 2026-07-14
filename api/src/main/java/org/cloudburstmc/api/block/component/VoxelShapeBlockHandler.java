package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;

/**
 * Supplies a voxel shape for a block state.
 */
@FunctionalInterface
public interface VoxelShapeBlockHandler {

    /**
     * Gets the shape for a block state and collision context.
     *
     * @param state the block state
     * @param context the collision context
     * @return the shape for the block
     */
    VoxelShape execute(BlockState state, CollisionContext context);
}
