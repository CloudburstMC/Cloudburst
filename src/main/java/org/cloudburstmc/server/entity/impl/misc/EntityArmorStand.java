package org.cloudburstmc.server.entity.impl.misc;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.misc.ArmorStand;
import org.cloudburstmc.server.level.Location;

public class EntityArmorStand extends BaseEntity implements ArmorStand {

    public EntityArmorStand(EntityType<?> type, Location location) {
        super(type, location);
    }
}
