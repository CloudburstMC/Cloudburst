package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.LlamaSpit;
import org.cloudburstmc.api.level.Location;

public class EntityLlamaSpit extends EntityProjectile implements LlamaSpit {
    public EntityLlamaSpit(EntityType<?> type, Location location) {
        super(type, location);
    }
}
