package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;

/**
 * Calculates block collision geometry from block, level, and entity context.
 */
@FunctionalInterface
public interface CollisionShapeHandler {

    /**
     * @param state the block state
     * @param blockContext the level and position of the block, when available
     * @param collisionContext the entity-specific collision context
     * @return collision geometry in block-local coordinates
     */
    VoxelShape execute(BlockState state, BlockShapeContext blockContext, CollisionContext collisionContext);

    /**
     * @return the mutable external state that can affect this handler
     */
    default ShapeContextRequirement contextRequirement() {
        return ShapeContextRequirement.STATE_ONLY;
    }
}
