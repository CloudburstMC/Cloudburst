package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;

import java.util.Objects;

/**
 * Fired before an entity creates an explosion.
 */
public class ExplosionPrimeEvent extends EntityEvent implements Cancellable {

    private float radius;
    private ExplosionBlockInteraction blockInteraction;
    private boolean causesFire;

    public ExplosionPrimeEvent(Entity entity, float radius, ExplosionBlockInteraction blockInteraction) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.setRadius(radius);
        this.blockInteraction = Objects.requireNonNull(blockInteraction, "blockInteraction");
    }

    /**
     * Returns the blast radius before the explosion is applied.
     */
    public float getRadius() {
        return this.radius;
    }

    /**
     * Changes the blast radius.
     */
    public void setRadius(float radius) {
        if (!Float.isFinite(radius) || radius < 0) {
            throw new IllegalArgumentException("Explosion radius must be finite and non-negative");
        }

        this.radius = radius;
    }

    /**
     * Returns how the explosion will affect blocks.
     */
    public ExplosionBlockInteraction getBlockInteraction() {
        return this.blockInteraction;
    }

    /**
     * Changes how the explosion will affect blocks.
     */
    public void setBlockInteraction(ExplosionBlockInteraction blockInteraction) {
        this.blockInteraction = Objects.requireNonNull(blockInteraction, "blockInteraction");
    }

    /**
     * Returns whether the explosion may ignite nearby blocks.
     */
    public boolean causesFire() {
        return this.causesFire;
    }

    /**
     * Changes whether the explosion may ignite nearby blocks.
     */
    public void setCausesFire(boolean causesFire) {
        this.causesFire = causesFire;
    }
}
