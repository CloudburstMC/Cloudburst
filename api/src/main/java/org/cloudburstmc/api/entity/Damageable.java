package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Represents an entity with health that can take damage.
 */
public interface Damageable {

    /**
     * Applies generic damage to this entity.
     *
     * @param amount the non-negative damage amount
     * @return whether damage was applied
     */
    default boolean damage(float amount) {
        return this.damage(amount, DamageSource.of(DamageTypes.GENERIC));
    }

    /**
     * Applies damage attributed directly to an entity.
     *
     * @param amount the non-negative damage amount
     * @param source the entity responsible for the damage
     * @return whether damage was applied
     */
    default boolean damage(float amount, Entity source) {
        Entity attacker = requireNonNull(source, "source");
        DamageSource damageSource = DamageSource.of(attacker instanceof Player ? DamageTypes.PLAYER_ATTACK : DamageTypes.MOB_ATTACK, attacker);
        return this.damage(amount, damageSource);
    }

    /**
     * Applies damage from the supplied source.
     *
     * @param amount the non-negative damage amount
     * @param source the damage source
     * @return whether damage was applied
     */
    boolean damage(float amount, DamageSource source);

    /**
     * Restores health for the standard regeneration reason.
     *
     * @param amount the non-negative amount to restore
     */
    void heal(float amount);

    /**
     * Applies a configurable health-regain event.
     *
     * @param source the health-regain event
     */
    void heal(EntityRegainHealthEvent source);

    /**
     * Returns the current health.
     *
     * @return the current health
     */
    float getHealth();

    /**
     * Sets the current health.
     *
     * @param health the new health
     */
    void setHealth(float health);

    /**
     * Returns the maximum health.
     *
     * @return the maximum health
     */
    int getMaxHealth();

    /**
     * Sets the maximum health.
     *
     * @param maxHealth the new maximum health
     */
    void setMaxHealth(int maxHealth);

    /**
     * Returns whether this entity has health remaining.
     *
     * @return whether the entity is alive
     */
    default boolean isAlive() {
        return this.getHealth() > 0;
    }

    /**
     * Returns the last damage event applied to this entity.
     *
     * @return the last damage event, or {@code null}
     */
    @Nullable
    EntityDamageEvent getLastDamageCause();

    /**
     * Returns the current absorption health.
     *
     * @return the absorption amount
     */
    float getAbsorption();

    /**
     * Sets the current absorption health.
     *
     * @param absorption the new absorption amount
     */
    void setAbsorption(float absorption);
}
