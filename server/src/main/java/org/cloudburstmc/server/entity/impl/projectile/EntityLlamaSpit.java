package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.LlamaSpit;
import org.cloudburstmc.server.level.Location;

public class EntityLlamaSpit extends EntityProjectile implements LlamaSpit {
    public EntityLlamaSpit(EntityType<?> type, Location location) {
        super(type, location);
    }
}
