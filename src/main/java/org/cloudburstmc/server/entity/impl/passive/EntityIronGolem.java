package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.passive.IronGolem;
import org.cloudburstmc.server.level.Location;

public class EntityIronGolem extends BaseEntity implements IronGolem {

    public EntityIronGolem(EntityType<?> type, Location location) {
        super(type, location);
    }
}
