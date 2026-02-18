package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.TripodCamera;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntityTripodCamera extends CloudEntity implements TripodCamera {

    public EntityTripodCamera(EntityType<?> type, Location location) {
        super(type, location);
    }
}
