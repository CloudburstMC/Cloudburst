package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

public class BlockIgniteEvent extends BlockEvent implements Cancellable {

    private final Block source;
    private final Entity entity;
    private final BlockIgniteCause cause;

    public BlockIgniteEvent(Block block, Block source, Entity entity, BlockIgniteCause cause) {
        super(block);
        this.source = source;
        this.entity = entity;
        this.cause = cause;
    }

    public Block getSource() {
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
