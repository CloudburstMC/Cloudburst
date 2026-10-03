package org.cloudburstmc.api.event.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.KnockbackCause;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * Called before knockback changes an entity's motion. Cancellation prevents the push
 * without undoing damage or other effects of the hit.
 */
public class EntityKnockbackEvent extends EntityEvent implements Cancellable {

    private final KnockbackCause cause;
    private final @Nullable Entity sourceEntity;
    private Vector3f knockback;

    /**
     * @param entity       the entity receiving knockback
     * @param cause        the reason for the push
     * @param sourceEntity the entity responsible, or {@code null} for an environmental cause
     * @param knockback    the proposed change in motion after resistance
     */
    public EntityKnockbackEvent(Entity entity, KnockbackCause cause, @Nullable Entity sourceEntity, Vector3f knockback) {
        this.entity = requireNonNull(entity, "entity");
        this.cause = requireNonNull(cause, "cause");
        this.sourceEntity = sourceEntity;
        this.setKnockback(knockback);
    }

    /**
     * @return the reason for the push
     */
    public KnockbackCause getCause() {
        return this.cause;
    }

    /**
     * @return the responsible entity, or {@code null} for an environmental cause
     */
    public @Nullable Entity getSourceEntity() {
        return this.sourceEntity;
    }

    /**
     * @return the change in motion in blocks per tick, not the resulting total motion
     */
    public Vector3f getKnockback() {
        return this.knockback;
    }

    /**
     * Changes the push without changing the entity's existing motion.
     *
     * @param knockback the finite change in motion
     * @throws IllegalArgumentException if any component is not finite
     */
    public void setKnockback(Vector3f knockback) {
        requireNonNull(knockback, "knockback");
        if (!Float.isFinite(knockback.getX()) || !Float.isFinite(knockback.getY()) || !Float.isFinite(knockback.getZ())) {
            throw new IllegalArgumentException("Knockback must be finite");
        }
        this.knockback = knockback;
    }
}
