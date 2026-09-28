package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Called before an entity changes a block when no more specific event applies.
 * Cancelling prevents the proposed change. For tool transformations, it also
 * prevents drops, item consumption and durability loss.
 */
public class EntityChangeBlockEvent extends EntityEvent implements Cancellable {

    private final Block block;
    private final BlockState to;

    /**
     * @param entity entity responsible for the change
     * @param block  block before the change
     * @param to     proposed replacement state
     */
    public EntityChangeBlockEvent(Entity entity, Block block, BlockState to) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.block = Objects.requireNonNull(block, "block");
        this.to = Objects.requireNonNull(to, "to");
    }

    /**
     * @return block before the change
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * @return proposed replacement block type
     */
    public BlockType getTo() {
        return this.to.getType();
    }

    /**
     * @return immutable proposed replacement state, including its traits
     */
    public BlockState getBlockState() {
        return this.to;
    }
}
