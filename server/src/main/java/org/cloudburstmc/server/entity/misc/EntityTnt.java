package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.level.Location;

public class EntityTnt extends EntityPrimedTnt {

    public EntityTnt(EntityType<PrimedTnt> type, Location location) {
        super(type, location);
    }
}
