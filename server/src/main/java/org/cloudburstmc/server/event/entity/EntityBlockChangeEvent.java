package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.entity.EntityEvent;
import org.cloudburstmc.server.block.BlockState;

/**
 * Created on 15-10-26.
 */
public class EntityBlockChangeEvent extends EntityEvent implements Cancellable {

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
