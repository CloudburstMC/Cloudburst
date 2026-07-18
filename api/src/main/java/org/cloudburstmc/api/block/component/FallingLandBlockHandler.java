package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.misc.FallingBlock;

/**
 * Handles block-specific behavior when a falling block lands.
 */
@FunctionalInterface
public interface FallingLandBlockHandler {

    /**
     * Applies block-specific behavior when a falling block reaches the ground.
     *
     * @param entity the falling block
     * @param target the block position where it attempts to land
     * @param fallDistance the distance fallen
     */
    void execute(FallingBlock entity, Block target, float fallDistance);
}
