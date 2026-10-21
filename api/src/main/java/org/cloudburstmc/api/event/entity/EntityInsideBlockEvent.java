package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Fired before a block responds to an entity inside it.
 * Cancellation prevents that response without changing the block's collision.
 * May be called repeatedly while contact continues. Blocks without an inside response do not call this event.
 */
public class EntityInsideBlockEvent extends EntityEvent implements Cancellable {

    private final Block block;

    /**
     * @param entity the entity inside the block
     * @param block  the block responding to the entity
     */
    public EntityInsideBlockEvent(Entity entity, Block block) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.block = Objects.requireNonNull(block, "block");
    }

    /**
     * @return the block containing the entity
     */
    public Block getBlock() {
        return this.block;
    }
}
