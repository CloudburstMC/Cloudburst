package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.EyeOfEnderSignal;
import org.cloudburstmc.server.world.Location;

public class EntityEyeOfEnderSignal extends EntityProjectile implements EyeOfEnderSignal {

    public EntityEyeOfEnderSignal(EntityType<?> type, Location location) {
        super(type, location);
    }
}
