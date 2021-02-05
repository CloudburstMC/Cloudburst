package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.ArmorStand;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.BaseEntity;

public class EntityArmorStand extends BaseEntity implements ArmorStand {

    public EntityArmorStand(EntityType<?> type, Location location) {
        super(type, location);
    }
}
