package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.IceBomb;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntityIceBomb extends CloudEntity implements IceBomb {

    public EntityIceBomb(EntityType<?> type, Location location) {
        super(type, location);
    }
}
