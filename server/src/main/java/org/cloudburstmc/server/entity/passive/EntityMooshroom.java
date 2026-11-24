package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Mooshroom;
import org.cloudburstmc.api.level.Location;

public class EntityMooshroom extends EntityAbstractCow implements Mooshroom {

    public EntityMooshroom(EntityType<Mooshroom> type, Location location) {
        super(type, location);
    }

    @Override
    protected String getDefaultName() {
        return "Mooshroom";
    }
}
