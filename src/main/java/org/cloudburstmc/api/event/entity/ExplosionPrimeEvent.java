package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: Box
 * Nukkit Project
 * <p>
 * Called when a entity decides to explode
 */
public final class ExplosionPrimeEvent extends EntityEvent implements Cancellable {

    protected double force;
    private boolean blockBreaking;

    public ExplosionPrimeEvent(Entity entity, double force) {
        this.entity = entity;
        this.force = force;
        this.blockBreaking = true;
    }

    public double getForce() {
        return this.force;
    }

    public void setForce(double force) {
        this.force = force;
    }

    public boolean isBlockBreaking() {
        return this.blockBreaking;
    }


    public void setBlockBreaking(boolean affectsBlocks) {
        this.blockBreaking = affectsBlocks;
    }
}
