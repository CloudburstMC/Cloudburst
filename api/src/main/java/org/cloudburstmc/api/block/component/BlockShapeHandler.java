package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.VoxelShape;

/**
 * Calculates block-local geometry, optionally using an explicit level context.
 */
@FunctionalInterface
public interface BlockShapeHandler {

    /**
     * @param state the block state
     * @param context the query context
     * @return geometry in block-local coordinates
     */
    VoxelShape execute(BlockState state, BlockShapeContext context);

    /**
     * Declares which mutable external state can affect this handler's result.
     *
     * @return the strongest context requirement
     */
    default ShapeContextRequirement contextRequirement() {
        return ShapeContextRequirement.STATE_ONLY;
    }
}
