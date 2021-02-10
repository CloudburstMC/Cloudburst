package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class EntityDamageByChildEntityEvent extends EntityDamageByEntityEvent {

    private final Entity childEntity;

    public EntityDamageByChildEntityEvent(Entity damager, Entity childEntity, Entity entity, DamageCause cause, float damage) {
        super(damager, entity, cause, damage);
        this.childEntity = childEntity;
    }

    public Entity getChild() {
        return childEntity;
    }
}
