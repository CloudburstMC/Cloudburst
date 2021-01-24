package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.SmallFireball;
import org.cloudburstmc.server.world.Location;

public class EntitySmallFireball extends EntityProjectile implements SmallFireball {
    public EntitySmallFireball(EntityType<?> type, Location location) {
        super(type, location);
    }
}
