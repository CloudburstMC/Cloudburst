package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.Fireball;
import org.cloudburstmc.server.level.Location;

public class EntityFireball extends EntityProjectile implements Fireball {
    public EntityFireball(EntityType<?> type, Location location) {
        super(type, location);
    }
}
