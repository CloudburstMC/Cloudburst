package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.util.MovingObjectPosition;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class ProjectileHitEvent extends EntityEvent implements Cancellable {

    private MovingObjectPosition movingObjectPosition;

    public ProjectileHitEvent(Projectile entity) {
        this(entity, null);
    }

    public ProjectileHitEvent(Projectile entity, MovingObjectPosition movingObjectPosition) {
        this.entity = entity;
        this.movingObjectPosition = movingObjectPosition;
    }

    public MovingObjectPosition getMovingObjectPosition() {
        return movingObjectPosition;
    }

    public void setMovingObjectPosition(MovingObjectPosition movingObjectPosition) {
        this.movingObjectPosition = movingObjectPosition;
    }

}
