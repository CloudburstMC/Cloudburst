package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.CommandBlockMinecart;
import org.cloudburstmc.server.level.Location;

public class EntityCommandBlockMinecart extends EntityVehicle implements CommandBlockMinecart {

    public EntityCommandBlockMinecart(EntityType<CommandBlockMinecart> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean mount(Entity entity) {
        return false;
    }

    @Override
    public boolean dismount(Entity vehicle) {
        return false;
    }
}
