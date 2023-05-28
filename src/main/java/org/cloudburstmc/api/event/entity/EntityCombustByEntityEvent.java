package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class EntityCombustByEntityEvent extends EntityCombustEvent {

    protected final Entity combuster;

    public EntityCombustByEntityEvent(Entity combuster, Entity combustee, int duration) {
        super(combustee, duration);
        this.combuster = combuster;
    }

    public Entity getCombuster() {
        return combuster;
    }
}
