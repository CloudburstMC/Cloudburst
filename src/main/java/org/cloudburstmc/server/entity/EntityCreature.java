package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
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
