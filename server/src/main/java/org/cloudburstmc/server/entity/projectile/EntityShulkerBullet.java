package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.ShulkerBullet;
import org.cloudburstmc.api.level.Location;

public class EntityShulkerBullet extends EntityProjectile implements ShulkerBullet {
    public EntityShulkerBullet(EntityType<?> type, Location location) {
        super(type, location);
    }
}
