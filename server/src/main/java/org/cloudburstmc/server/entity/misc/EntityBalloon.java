package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.Balloon;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntityBalloon extends CloudEntity implements Balloon {

    public EntityBalloon(EntityType<?> type, Location location) {
        super(type, location);
    }
}
