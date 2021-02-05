package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityCreature extends EntityLiving implements Creature {

    public EntityCreature(EntityType<?> type, Location location) {
        super(type, location);
    }
}
