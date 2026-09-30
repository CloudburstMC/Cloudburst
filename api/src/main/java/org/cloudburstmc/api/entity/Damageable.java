package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Represents an entity with health that can take damage.
 */
public interface Damageable {

    /**
     * Applies generic damage to this entity.
     *
     * @param amount the finite, non-negative damage amount
     * @return whether the hit was accepted, even if reductions prevented health loss
     * @throws IllegalArgumentException if the amount is negative or not finite
     */
    default boolean damage(float amount) {
        return this.damage(amount, DamageSource.of(DamageTypes.GENERIC));
    }

    /**
     * Applies damage attributed directly to an entity.
     *
     * @param amount the finite, non-negative damage amount
     * @param source the entity responsible for the damage
     * @return whether the hit was accepted, even if reductions prevented health loss
     * @throws IllegalArgumentException if the amount is negative or not finite
     */
    default boolean damage(float amount, Entity source) {
        Entity attacker = requireNonNull(source, "source");
        DamageSource damageSource = DamageSource.builder(attacker instanceof Player ? DamageTypes.PLAYER_ATTACK : DamageTypes.MOB_ATTACK)
                .directEntity(attacker)
                .causingEntity(attacker)
                .weaponItem(attacker instanceof Player player ? player.getInventory().getSelectedItem() : ItemStack.EMPTY)
                .build();
        return this.damage(amount, damageSource);
    }

    /**
     * Applies damage from the supplied source.
     * Fully blocked hits on living entities return {@code false}, even when they trigger blocking effects.
     *
     * @param amount the finite, non-negative damage amount
     * @param source the damage source
     * @return whether the hit was accepted, even if reductions prevented health loss
     * @throws IllegalArgumentException if the amount is negative or not finite
     */
    boolean damage(float amount, DamageSource source);

    /**
     * Restores health for the custom healing reason.
     *
     * @param amount the finite, non-negative amount to restore
     * @throws IllegalArgumentException if the amount is negative or not finite
     */
    default void heal(float amount) {
        this.heal(amount, RegainReason.CUSTOM);
    }

    /**
     * Restores health for the supplied reason, firing a cancellable health-regain event.
     * Closed or dead entities cannot be healed. The resulting health is capped at the maximum.
     *
     * @param amount the finite, non-negative amount to restore
     * @param reason the cause of healing
     * @throws IllegalArgumentException if the amount is negative or not finite
     */
    void heal(float amount, RegainReason reason);

    /**
     * Returns the current health.
     *
     * @return the current health
     */
    float getHealth();

    /**
     * Sets the current health, capped at the maximum. Zero requests death.
     * Cancelling a living entity's death event restores its configured revival health.
     *
     * @param health the finite, non-negative health
     * @throws IllegalArgumentException if health is negative or not finite
     */
    void setHealth(float health);

    /**
     * Returns the effective maximum health, including active health-boost effects.
     *
     * @return the maximum health
     */
    int getMaxHealth();

    /**
     * Sets the base maximum health, before health-boost effects.
     * Reducing the effective maximum also caps current health. Increasing it does not heal.
     *
     * @param maxHealth the positive base maximum health
     * @throws IllegalArgumentException if the maximum is not positive
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
     * @param absorption the finite, non-negative absorption amount
     * @throws IllegalArgumentException if absorption is negative or not finite
     */
    void setAbsorption(float absorption);
}
