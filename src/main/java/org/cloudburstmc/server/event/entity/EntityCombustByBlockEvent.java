package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * author: Box
 * Nukkit Project
 */
public class EntityCombustByBlockEvent extends EntityCombustEvent {

    protected final Block combustor;

    public EntityCombustByBlockEvent(Block combustor, Entity combustee, int duration) {
        super(combustee, duration);
        this.combustor = combustor;
    }

    public Block getCombustor() {
        return combustor;
    }
}
