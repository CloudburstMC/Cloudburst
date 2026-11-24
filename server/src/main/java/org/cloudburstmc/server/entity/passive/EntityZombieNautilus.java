package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.ZombieNautilus;
import org.cloudburstmc.api.level.Location;

public class EntityZombieNautilus extends EntityAbstractNautilus implements ZombieNautilus {

    public EntityZombieNautilus(EntityType<ZombieNautilus> type, Location location) {
        super(type, location);
    }

    @Override
    protected String getDefaultName() {
        return "Zombie Nautilus";
    }
}
