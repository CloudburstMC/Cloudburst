package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.WitherSkull;
import org.cloudburstmc.server.level.Location;

public class EntityWitherSkull extends EntityProjectile implements WitherSkull {
    public EntityWitherSkull(EntityType<?> type, Location location) {
        super(type, location);
    }
}
