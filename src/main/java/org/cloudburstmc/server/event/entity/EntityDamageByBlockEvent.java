package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityDamageByBlockEvent extends EntityDamageEvent {

    private final BlockState damager;

    public EntityDamageByBlockEvent(BlockState damager, Entity entity, DamageCause cause, float damage) {
        super(entity, cause, damage);
        this.damager = damager;
    }

    public BlockState getDamager() {
        return damager;
    }

}
