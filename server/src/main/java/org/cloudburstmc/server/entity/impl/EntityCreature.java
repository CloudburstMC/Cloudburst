package org.cloudburstmc.server.entity.impl;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.level.Location;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityCreature extends EntityLiving {

    public EntityCreature(EntityType<?> type, Location location) {
        super(type, location);
    }
}
