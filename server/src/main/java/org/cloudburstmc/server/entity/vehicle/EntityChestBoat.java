package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.ChestBoat;
import org.cloudburstmc.api.level.Location;

public class EntityChestBoat extends EntityBoat implements ChestBoat {

    public EntityChestBoat(EntityType<ChestBoat> type, Location location) {
        super(type, location);
    }
}
