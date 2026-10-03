package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
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
     * Requests adding a potion effect or replacing an active effect of the same type.
     * The potion-effect change event can cancel the change or control replacement.
     *
     * @param effect effect to add
     * @return {@code true} when the active effects changed
     */
    boolean addPotionEffect(PotionEffect effect);

    /**
     * Removes an active potion effect unless its change event is cancelled.
     *
     * @param type effect type
     * @return {@code true} when an effect was removed
     */
    boolean removePotionEffect(EffectType type);

    /**
     * Requests removal of every active potion effect. Effects whose change events
     * are cancelled remain active.
     *
     * @return {@code true} when at least one effect was removed
     */
    boolean clearActivePotionEffects();

    /**
     * Returns the item currently protecting this entity, after its raising delay.
     *
     * @return the blocking item, or {@link ItemStack#EMPTY} when not blocking
     */
    ItemStack getBlockingItem();

    /**
     * Returns whether this entity is ready to block attacks with a raised item.
     *
     * @return whether item blocking is active
     */
    boolean isBlocking();

    /**
     * Returns whether this entity is in a climbing state on a climbable surface.
     *
     * @return whether the entity is climbing
     */
    boolean isClimbing();

    /**
     * Performs this entity's standard attack against a target.
     *
     * @param target the entity to attack
     * @return whether the attack damaged the target
     */
    boolean attack(Entity target);
}
