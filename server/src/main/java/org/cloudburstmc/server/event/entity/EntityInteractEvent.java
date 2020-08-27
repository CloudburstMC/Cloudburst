package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;

/**
 * @author CreeperFace
 */
public class EntityInteractEvent extends EntityEvent implements Cancellable {

    private final Block block;

    public EntityInteractEvent(Entity entity, Block block) {
        this.entity = entity;
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
