package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

/**
 * @author CreeperFace
 */
public final class EntityInteractEvent extends EntityEvent implements Cancellable {

    private final Block block;

    public EntityInteractEvent(Entity entity, Block block) {
        this.entity = entity;
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
