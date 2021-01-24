package org.cloudburstmc.server.entity.impl.vehicle;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.vehicle.CommandBlockMinecart;
import org.cloudburstmc.server.world.Location;

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
