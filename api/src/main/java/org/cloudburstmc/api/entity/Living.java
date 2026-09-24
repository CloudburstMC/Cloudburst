package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.PotionEffect;

import java.util.Map;

/**
 * An entity with health and other living-entity behavior.
 */
public interface Living extends Entity, ProjectileSource {

    /**
     * Returns the active potion effects keyed by type. The returned effects are
     * detached snapshots and cannot mutate entity state.
     *
     * @return immutable snapshot of active potion effects
     */
    Map<EffectType, PotionEffect> getActivePotionEffects();

    /**
     * Returns a detached snapshot of the active potion effect of a type.
     *
     * @param type effect type
     * @return active potion effect, or {@code null}
     */
    @Nullable
    PotionEffect getPotionEffect(EffectType type);

    /**
     * Returns whether a potion effect type is active.
     *
     * @param type effect type
     * @return {@code true} when the effect is active
     */
    boolean hasPotionEffect(EffectType type);

    /**
     * Adds a potion effect or replaces a weaker active effect of the same type.
     *
     * @param effect effect to add
     * @return {@code true} when the active effects changed
     */
    boolean addPotionEffect(PotionEffect effect);

    /**
     * Removes an active potion effect.
     *
     * @param type effect type
     * @return {@code true} when an effect was removed
     */
    boolean removePotionEffect(EffectType type);

    /**
     * Removes every active potion effect from this entity.
     *
     * @return {@code true} when at least one effect was removed
     */
    boolean clearActivePotionEffects();

    /**
     * Performs this entity's standard attack against a target.
     *
     * @param target the entity to attack
     * @return whether the attack damaged the target
     */
    boolean attack(Entity target);
}
