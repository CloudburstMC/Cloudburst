package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * Handles an entity being inside a block.
 */
@FunctionalInterface
public interface EntityInsideBlockHandler {

    /**
     * Runs the block's inside-entity behavior.
     *
     * @param block the block containing the entity
     * @param entity the entity inside the block
     * @param precise whether precise inside-block collision checks were used
     */
    void execute(Block block, Entity entity, boolean precise);
}
