package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.LeashKnot;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.level.Location;

public class EntityLeashKnot extends BaseEntity implements LeashKnot {
    public EntityLeashKnot(EntityType<?> type, Location location) {
        super(type, location);
    }
}
