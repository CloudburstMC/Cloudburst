package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.block.Block;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityDamageByBlockEvent extends EntityDamageEvent {

    private final Block damager;

    public EntityDamageByBlockEvent(Block damager, Entity entity, DamageCause cause, float damage) {
        super(entity, cause, damage);
        this.damager = damager;
    }

    public Block getDamager() {
        return damager;
    }

}
