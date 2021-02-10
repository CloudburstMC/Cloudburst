package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

/**
 * Created on 15-10-26.
 */
public final class EntityBlockChangeEvent extends EntityEvent implements Cancellable {

    private final Block block;
    private final BlockState to;

    public EntityBlockChangeEvent(Entity entity, Block block, BlockState to) {
        this.entity = entity;
        this.block = block;
        this.to = to;
    }

    public Block getBlock() {
        return block;
    }

    public BlockState getTo() {
        return to;
    }

}
