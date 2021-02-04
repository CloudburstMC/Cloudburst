package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.SnowGolem;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.level.Location;

public class EntitySnowGolem extends BaseEntity implements SnowGolem {

    public EntitySnowGolem(EntityType<?> type, Location location) {
        super(type, location);
    }
}
