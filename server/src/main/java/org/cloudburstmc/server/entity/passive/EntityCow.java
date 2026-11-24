package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Cow;
import org.cloudburstmc.api.level.Location;

public class EntityCow extends EntityAbstractCow implements Cow {

    public EntityCow(EntityType<Cow> type, Location location) {
        super(type, location);
    }

    @Override
    protected String getDefaultName() {
        return "Cow";
    }
}
