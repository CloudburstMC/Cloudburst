package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.WindChargeProjectile;
import org.cloudburstmc.api.level.Location;

public class EntityWindChargeProjectile extends EntityProjectile implements WindChargeProjectile {

    public EntityWindChargeProjectile(EntityType<?> type, Location location) {
        super(type, location);
    }
}
