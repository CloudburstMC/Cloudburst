package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.ShulkerBullet;
import org.cloudburstmc.server.level.Location;

public class EntityShulkerBullet extends EntityProjectile implements ShulkerBullet {
    public EntityShulkerBullet(EntityType<?> type, Location location) {
        super(type, location);
    }
}
