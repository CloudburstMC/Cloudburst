package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.IronGolem;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.BaseEntity;

public class EntityIronGolem extends BaseEntity implements IronGolem {

    public EntityIronGolem(EntityType<?> type, Location location) {
        super(type, location);
    }
}
