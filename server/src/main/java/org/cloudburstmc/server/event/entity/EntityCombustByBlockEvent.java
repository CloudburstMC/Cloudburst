package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;

/**
 * author: Box
 * Nukkit Project
 */
public class EntityCombustByBlockEvent extends EntityCombustEvent {

    protected final BlockState combuster;

    public EntityCombustByBlockEvent(BlockState combuster, Entity combustee, int duration) {
        super(combustee, duration);
        this.combuster = combuster;
    }

    public BlockState getCombuster() {
        return combuster;
    }
}
