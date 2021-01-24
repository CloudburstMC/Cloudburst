package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.DragonFireball;
import org.cloudburstmc.server.world.Location;

public class EntityDragonFireball extends EntityProjectile implements DragonFireball {
    public EntityDragonFireball(EntityType<?> type, Location location) {
        super(type, location);
    }
}
