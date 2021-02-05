package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.util.MovingObjectPosition;
import org.cloudburstmc.server.entity.projectile.EntityProjectile;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ProjectileHitEvent extends EntityEvent implements Cancellable {

    private MovingObjectPosition movingObjectPosition;

    public ProjectileHitEvent(EntityProjectile entity) {
        this(entity, null);
    }

    public ProjectileHitEvent(EntityProjectile entity, MovingObjectPosition movingObjectPosition) {
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
