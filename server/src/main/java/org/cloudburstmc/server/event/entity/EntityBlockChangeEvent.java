package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

/**
 * Created on 15-10-26.
 */
public class EntityBlockChangeEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final BlockState from;
    private final BlockState to;

    public EntityBlockChangeEvent(Entity entity, BlockState from, BlockState to) {
        this.entity = entity;
        this.from = from;
        this.to = to;
    }

    public BlockState getFrom() {
        return from;
    }

    public BlockState getTo() {
        return to;
    }

}
