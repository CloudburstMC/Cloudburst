package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.EvokerFangs;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntityEvokerFangs extends CloudEntity implements EvokerFangs {

    public EntityEvokerFangs(EntityType<?> type, Location location) {
        super(type, location);
    }
}
