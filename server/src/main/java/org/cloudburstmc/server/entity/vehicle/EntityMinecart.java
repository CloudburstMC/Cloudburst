package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.Minecart;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.entity.passive.EntityWaterAnimal;
import org.cloudburstmc.server.player.CloudPlayer;

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
    public boolean onUpdate(int currentTick) {
        boolean update = super.onUpdate(currentTick);
        if (this.closed || !this.isAlive()) {
            return false;
        }

        if (this.passengers.isEmpty()) {
            for (Entity entity : this.getLevel().getCollidingEntities(this, this.boundingBox.inflate(0.2f, 0, 0.2f))) {
                if (entity.getVehicle() != null || !(entity instanceof EntityLiving) || entity instanceof CloudPlayer || entity instanceof EntityWaterAnimal) {
                    continue;
                }

                if (((CloudEntity) entity).tryMount(this)) {
                    update = true;
                    break;
                }
            }
        }

        return update;
    }

    @Override
    protected void activate(int x, int y, int z, boolean powered) {
        if (!powered) {
            return;
        }

        this.ejectPassengers();
        if (this.getRollingAmplitude() == 0) {
            this.performHurtAnimation();
            this.setDamage(50);
        }
    }
}
