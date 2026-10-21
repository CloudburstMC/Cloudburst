package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.VoxelShape;

/**
 * Supplies block-local geometry that obstructs light between adjacent blocks.
 */
@FunctionalInterface
public interface LightOcclusionShapeHandler {

    /**
     * Resolves geometry independently of collision and selection shapes.
     *
     * @param state the block state
     * @return the occluding shape, or an empty shape for opacity-only lighting
     */
    VoxelShape execute(BlockState state);
}
