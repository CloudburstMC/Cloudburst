package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * Handles an interaction between a block and an entity.
 */
@FunctionalInterface
public interface EntityBlockHandler {

    /**
     * @param block the participating block
     * @param entity the participating entity
     */
    void execute(Block block, Entity entity);
}
