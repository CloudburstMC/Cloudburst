package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * Handles an entity landing on a block.
 */
@FunctionalInterface
public interface FallOnBlockHandler {

    /**
     * @param block the block landed on
     * @param entity the landing entity
     * @param fallDistance the distance fallen in blocks
     */
    void execute(Block block, Entity entity, float fallDistance);
}
