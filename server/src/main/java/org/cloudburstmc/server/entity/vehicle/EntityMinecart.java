package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.Minecart;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.entity.passive.EntityWaterAnimal;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.data.MinecartType;

/**
 * Created by Snake1999 on 2016/1/30.
 * Package cn.nukkit.entity.item in project Nukkit.
 */
public class EntityMinecart extends EntityAbstractMinecart implements Minecart {

    public EntityMinecart(EntityType<Minecart> type, Location location) {
        super(type, location);
    }

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.valueOf(0);
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    protected void activate(int x, int y, int z, boolean flag) {
        if (flag) {
            if (this.vehicle != null) {
                mount(vehicle);
            }
            // looks like MCPE and MCPC not same XD
            // removed rolling feature from here because of MCPE logic?
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean update = super.onUpdate(currentTick);

        if (this.passengers.isEmpty()) {
            for (Entity entity : this.getLevel().getCollidingEntities(this.boundingBox.grow(0.2f, 0, 0.2f), this)) {
                if (entity.getVehicle() != null || !(entity instanceof EntityLiving) || entity instanceof CloudPlayer || entity instanceof EntityWaterAnimal) {
                    continue;
                }

                entity.mount(this);
                update = true;
                break;
            }
        }

        return update;
    }
}
