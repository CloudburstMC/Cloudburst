package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;

import static java.util.Objects.requireNonNull;

/**
 * Called before an entity causes a block to form.
 * Cancellation prevents formation.
 */
public class EntityBlockFormEvent extends BlockFormEvent {

    private final Entity entity;

    /**
     * Creates an entity block-form event.
     *
     * @param entity   the entity forming the block
     * @param block    the block being changed
     * @param newState the state that will be formed
     */
    public EntityBlockFormEvent(Entity entity, Block block, BlockState newState) {
        super(block, newState);
        this.entity = requireNonNull(entity, "entity");
    }

    /**
     * Returns the entity forming the block.
     *
     * @return the entity forming the block
     */
    public Entity getEntity() {
        return this.entity;
    }
}
