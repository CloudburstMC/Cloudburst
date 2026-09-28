package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.event.Cancellable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Called before damage is applied. Cancellation prevents damage and its associated effects.
 * Defenses are captured for the hit. Changing the raw damage recalculates blocking
 * and damage reductions using those defenses.
 */
public abstract class EntityDamageEvent extends EntityEvent implements Cancellable {

    private final DamageSource damageSource;
    private final float originalDamage;
    private float damage;

    /**
     * Creates an event for damage from the supplied source.
     *
     * @param entity       the damaged entity
     * @param damageSource the damage source
     * @param damage       the non-negative incoming damage before reductions
     */
    protected EntityDamageEvent(Entity entity, DamageSource damageSource, float damage) {
        this.entity = requireNonNull(entity, "entity");
        this.damageSource = requireNonNull(damageSource, "damageSource");
        checkArgument(Float.isFinite(damage) && damage >= 0, "damage must be finite and non-negative");
        this.originalDamage = damage;
        this.damage = damage;
    }

    /**
     * Returns the kind of damage represented by this event.
     *
     * @return the damage type
     */
    public DamageType getDamageType() {
        return this.damageSource.getDamageType();
    }

    /**
     * Returns the source of the damage.
     *
     * @return the damage source
     */
    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    /**
     * Returns the damage supplied when this event was created.
     *
     * @return the original damage
     */
    public float getOriginalDamage() {
        return this.originalDamage;
    }

    /**
     * Returns the incoming damage before blocking and other reductions.
     *
     * @return the current damage
     */
    public float getDamage() {
        return this.damage;
    }

    /**
     * Sets the incoming damage before blocking and other reductions.
     *
     * @param damage the new non-negative damage
     */
    public void setDamage(float damage) {
        checkArgument(Float.isFinite(damage) && damage >= 0, "damage must be finite and non-negative");
        this.damage = damage;
    }

    /**
     * Returns the damage prevented by a raised item before other reductions.
     * This is recalculated when raw damage changes.
     *
     * @return the blocked damage
     */
    public abstract float getBlockedDamage();

    /**
     * Returns the damage prevented by absorption hearts after other reductions.
     * This is recalculated when raw damage changes.
     *
     * @return the absorbed damage
     */
    public abstract float getAbsorbedDamage();

    /**
     * Returns the health damage after blocking, hurt cooldowns, armor, potion effects
     * and absorption. This is recalculated when raw damage changes.
     *
     * @return the reduced damage
     */
    public abstract float getFinalDamage();
}
