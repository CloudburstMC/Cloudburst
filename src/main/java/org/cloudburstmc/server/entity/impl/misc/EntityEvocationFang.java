package org.cloudburstmc.server.entity.impl.misc;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.misc.EvocationFang;
import org.cloudburstmc.server.world.Location;

public class EntityEvocationFang extends BaseEntity implements EvocationFang {
    public EntityEvocationFang(EntityType<?> type, Location location) {
        super(type, location);
    }
}
