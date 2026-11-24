package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.Minecart;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.entity.passive.EntityWaterAnimal;
import org.cloudburstmc.server.player.CloudPlayer;

public class EntityMinecart extends EntityAbstractMinecart {

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
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean update = super.tickEntity(currentTick, elapsedTicks);
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        if (this.getPassengers().isEmpty()) {
            for (Entity entity : this.getLevel().getCollidingEntities(this, this.boundingBox.inflate(0.2f, 0, 0.2f))) {
                if (entity.getVehicle() != null || !(entity instanceof CloudLivingEntity) || entity instanceof CloudPlayer || entity instanceof EntityWaterAnimal) {
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
        if (this.getHurtTime() == 0) {
            this.performHurtAnimation();
            this.setDamage(50);
        }
    }
}
