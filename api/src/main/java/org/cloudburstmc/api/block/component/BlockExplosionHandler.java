package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * Handles a block's reaction before it is destroyed by an explosion.
 */
@FunctionalInterface
public interface BlockExplosionHandler {

    /**
     * @param block       the block to be destroyed
     * @param cause       the direct source entity, or {@code null}
     * @param sourceBlock the source block, or {@code null}
     * @return {@code true} to destroy the block, or {@code false} to leave it intact
     */
    boolean execute(Block block, @Nullable Entity cause, @Nullable Block sourceBlock);
}
