package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.LlamaSpit;
import org.cloudburstmc.server.level.Location;

public class EntityLlamaSpit extends EntityProjectile implements LlamaSpit {
    public EntityLlamaSpit(EntityType<?> type, Location location) {
        super(type, location);
    }
}
