package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.ArmorStand;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.level.Location;

public class EntityArmorStand extends BaseEntity implements ArmorStand {

    public EntityArmorStand(EntityType<?> type, Location location) {
        super(type, location);
    }
}
