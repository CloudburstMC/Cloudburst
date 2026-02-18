package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.WitherSkull;
import org.cloudburstmc.api.level.Location;

public class EntityWitherSkullDangerous extends EntityProjectile implements WitherSkull {

    public EntityWitherSkullDangerous(EntityType<?> type, Location location) {
        super(type, location);
    }
}
