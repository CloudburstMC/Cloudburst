package org.cloudburstmc.server.entity.impl.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.EvocationFang;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.level.Location;

public class EntityEvocationFang extends BaseEntity implements EvocationFang {
    public EntityEvocationFang(EntityType<?> type, Location location) {
        super(type, location);
    }
}
