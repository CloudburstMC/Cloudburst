package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

/**
 * @author CreeperFace
 */
public class EntityInteractEvent extends EntityEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private BlockState blockState;

    public EntityInteractEvent(Entity entity, BlockState blockState) {
        this.entity = entity;
        this.blockState = blockState;
    }

    public BlockState getBlockState() {
        return blockState;
    }
}
