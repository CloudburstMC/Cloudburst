package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.BreezeWindChargeProjectile;
import org.cloudburstmc.api.level.Location;

public class EntityBreezeWindChargeProjectile extends EntityProjectile implements BreezeWindChargeProjectile {

    public EntityBreezeWindChargeProjectile(EntityType<?> type, Location location) {
        super(type, location);
    }
}
