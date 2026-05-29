package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.CommandBlockMinecart;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MinecartType;

public class EntityCommandBlockMinecart extends EntityAbstractMinecart implements CommandBlockMinecart {

    public EntityCommandBlockMinecart(EntityType<CommandBlockMinecart> type, Location location) {
        super(type, location);
    }

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.MINECART_COMMAND_BLOCK;
    }

    @Override
    public boolean isRideable() {
        return false;
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
