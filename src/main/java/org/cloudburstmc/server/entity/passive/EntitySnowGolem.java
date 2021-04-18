package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.SnowGolem;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.BaseEntity;

public class EntitySnowGolem extends BaseEntity implements SnowGolem {

    public EntitySnowGolem(EntityType<?> type, Location location) {
        super(type, location);
    }
}
