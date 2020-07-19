package org.cloudburstmc.server.entity.impl.misc;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.misc.LeashKnot;
import org.cloudburstmc.server.level.Location;

public class EntityLeashKnot extends BaseEntity implements LeashKnot {
    public EntityLeashKnot(EntityType<?> type, Location location) {
        super(type, location);
    }
}
