package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.EvocationFang;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.BaseEntity;

public class EntityEvocationFang extends BaseEntity implements EvocationFang {
    public EntityEvocationFang(EntityType<?> type, Location location) {
        super(type, location);
    }
}
