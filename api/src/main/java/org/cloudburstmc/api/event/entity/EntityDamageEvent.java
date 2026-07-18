package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.event.Cancellable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Called when an entity takes damage.
 */
public final class EntityDamageEvent extends EntityEvent implements Cancellable {

    private final DamageSource damageSource;
    private final float originalDamage;
    private float damage;
    private float knockback = 1f;
    private int attackCooldown = 10;

    /**
     * Creates an event for damage without a specific origin.
     *
     * @param entity the damaged entity
     * @param damageType the damage type
     * @param damage the non-negative damage
     */
    public EntityDamageEvent(Entity entity, DamageType damageType, float damage) {
        this(entity, DamageSource.of(damageType), damage);
    }

    /**
     * Creates an event for damage from the supplied source.
     *
     * @param entity the damaged entity
     * @param damageSource the damage source
     * @param damage the non-negative damage
     */
    public EntityDamageEvent(Entity entity, DamageSource damageSource, float damage) {
        this.entity = requireNonNull(entity, "entity");
        this.damageSource = requireNonNull(damageSource, "damageSource");
        checkArgument(damage >= 0, "damage must not be negative");
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
     * Returns the damage that will be applied.
     *
     * @return the current damage
     */
    public float getDamage() {
        return this.damage;
    }

    /**
     * Sets the damage that will be applied.
     *
     * @param damage the new non-negative damage
     */
    public void setDamage(float damage) {
        checkArgument(damage >= 0, "damage must not be negative");
        this.damage = damage;
    }

    /**
     * Returns the multiplier applied to normal attack knockback.
     *
     * @return the knockback multiplier
     */
    public float getKnockback() {
        return this.knockback;
    }

    /**
     * Sets the multiplier applied to normal attack knockback.
     *
     * @param knockback the new non-negative multiplier
     */
    public void setKnockback(float knockback) {
        checkArgument(knockback >= 0, "knockback must not be negative");
        this.knockback = knockback;
    }

    /**
     * Returns the entity attack cooldown in ticks.
     *
     * @return the attack cooldown
     */
    public int getAttackCooldown() {
        return this.attackCooldown;
    }

    /**
     * Sets the entity attack cooldown in ticks.
     *
     * @param attackCooldown the new non-negative cooldown
     */
    public void setAttackCooldown(int attackCooldown) {
        checkArgument(attackCooldown >= 0, "attackCooldown must not be negative");
        this.attackCooldown = attackCooldown;
    }
}
