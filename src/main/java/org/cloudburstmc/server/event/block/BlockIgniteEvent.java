package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

public class BlockIgniteEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final BlockState source;
    private final Entity entity;
    private final BlockIgniteCause cause;

    public BlockIgniteEvent(Block block, BlockState source, Entity entity, BlockIgniteCause cause) {
        super(block);
        this.source = source;
        this.entity = entity;
        this.cause = cause;
    }

    public BlockState getSource() {
        return source;
    }

    public Entity getEntity() {
        return entity;
    }

    public BlockIgniteCause getCause() {
        return cause;
    }

    public enum BlockIgniteCause {
        EXPLOSION,
        FIREBALL,
        FLINT_AND_STEEL,
        LAVA,
        LIGHTNING,
        SPREAD
    }
}
