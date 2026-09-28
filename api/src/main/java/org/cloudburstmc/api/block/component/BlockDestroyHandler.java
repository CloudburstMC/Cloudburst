package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * Removes a block and applies any multi-block or neighbor effects of destruction.
 */
@FunctionalInterface
public interface BlockDestroyHandler {

    /**
     * @param block the block being destroyed
     * @param cause the responsible entity, or {@code null} for an environmental cause
     */
    void execute(Block block, @Nullable Entity cause);
}
