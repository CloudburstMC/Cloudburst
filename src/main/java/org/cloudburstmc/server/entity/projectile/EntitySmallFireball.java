package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.SmallFireball;
import org.cloudburstmc.server.level.Location;

public class EntitySmallFireball extends EntityProjectile implements SmallFireball {
    public EntitySmallFireball(EntityType<?> type, Location location) {
        super(type, location);
    }
}
