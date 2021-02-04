package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.EyeOfEnderSignal;
import org.cloudburstmc.server.level.Location;

public class EntityEyeOfEnderSignal extends EntityProjectile implements EyeOfEnderSignal {

    public EntityEyeOfEnderSignal(EntityType<?> type, Location location) {
        super(type, location);
    }
}
