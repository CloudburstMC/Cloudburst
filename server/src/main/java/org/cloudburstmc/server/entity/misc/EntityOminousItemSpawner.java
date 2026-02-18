package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.OminousItemSpawner;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntityOminousItemSpawner extends CloudEntity implements OminousItemSpawner {

    public EntityOminousItemSpawner(EntityType<?> type, Location location) {
        super(type, location);
    }
}
