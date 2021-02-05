package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.EyeOfEnderSignal;
import org.cloudburstmc.api.level.Location;

public class EntityEyeOfEnderSignal extends EntityProjectile implements EyeOfEnderSignal {

    public EntityEyeOfEnderSignal(EntityType<?> type, Location location) {
        super(type, location);
    }
}
