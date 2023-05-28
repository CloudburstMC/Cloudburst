package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class EntityDamageByBlockEvent extends EntityDamageEvent {

    private final Block damager;

    public EntityDamageByBlockEvent(Block damager, Entity entity, DamageCause cause, float damage) {
        super(entity, cause, damage);
        this.damager = damager;
    }

    public Block getDamager() {
        return damager;
    }

}
