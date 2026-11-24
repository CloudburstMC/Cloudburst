package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.EyeOfEnderSignal;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

public class EntityEyeOfEnderSignal extends CloudEntity implements EyeOfEnderSignal {

    public EntityEyeOfEnderSignal(EntityType<EyeOfEnderSignal> type, Location location) {
        super(type, location);
        this.noPhysics = true;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    protected boolean hasMovementEntityCollisions() {
        return false;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        return false;
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isAlive() && this.getMotion().lengthSquared() > 0) {
            this.move(this.getMotion().mul(elapsedTicks));
        }
        return super.tickEntity(currentTick, elapsedTicks);
    }
}
