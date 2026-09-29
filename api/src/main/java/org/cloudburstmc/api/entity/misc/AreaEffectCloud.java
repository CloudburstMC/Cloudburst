package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;

import java.util.List;

/**
 * An entity that repeatedly applies potion effects within a radius.
 */
public interface AreaEffectCloud extends Entity {
    /**
     * @return ticks before the cloud starts applying effects
     */
    int getWaitTime();

    /**
     * @param waitTime ticks before the cloud starts applying effects
     */
    void setWaitTime(int waitTime);

    /**
     * @return base potion type carried by the cloud
     */
    PotionType getPotionType();

    /**
     * @param potionType base potion type carried by the cloud
     */
    void setPotionType(PotionType potionType);

    /**
     * @return packed ARGB display color
     */
    int getPotionColor();

    /**
     * @param argb packed ARGB display color
     */
    void setPotionColor(int argb);

    /**
     * Sets the display color from individual color components.
     *
     * @param alpha alpha component from {@code 0} to {@code 255}
     * @param red   red component from {@code 0} to {@code 255}
     * @param green green component from {@code 0} to {@code 255}
     * @param blue  blue component from {@code 0} to {@code 255}
     */
    void setPotionColor(int alpha, int red, int green, int blue);

    /**
     * @return ticks before an entity may be affected again
     */
    int getReapplicationDelay();

    /**
     * @param reapplicationDelay ticks before an entity may be affected again
     */
    void setReapplicationDelay(int reapplicationDelay);

    /**
     * @return duration change after affecting an entity
     */
    int getDurationOnUse();

    /**
     * @param durationOnUse duration change after affecting an entity
     */
    void setDurationOnUse(int durationOnUse);

    /**
     * @return radius change after affecting an entity
     */
    float getRadiusOnUse();

    /**
     * @param radiusOnUse radius change after affecting an entity
     */
    void setRadiusOnUse(float radiusOnUse);

    /**
     * @return radius change applied each tick
     */
    float getRadiusPerTick();

    /**
     * @param radiusPerTick radius change applied each tick
     */
    void setRadiusPerTick(float radiusPerTick);

    /**
     * Returns the active duration after the wait time.
     *
     * @return duration in ticks, or {@link PotionEffect#INFINITE_DURATION}
     */
    int getDuration();

    /**
     * Sets the active duration after the wait time.
     *
     * @param duration duration in ticks, or {@link PotionEffect#INFINITE_DURATION}
     */
    void setDuration(int duration);

    /**
     * @return current effect radius
     */
    float getRadius();

    /**
     * @param radius current effect radius
     */
    void setRadius(float radius);

    /**
     * Returns the particle shown by this cloud.
     *
     * @return the particle type
     */
    ParticleType getParticle();

    /**
     * Sets the particle shown by this cloud.
     *
     * @param particle the particle type
     */
    void setParticle(ParticleType particle);

    /**
     * Returns the immutable custom effects carried by the cloud.
     *
     * @return immutable custom effects
     */
    List<PotionEffect> getCustomEffects();

    /**
     * Returns whether the cloud carries any custom effects.
     *
     * @return {@code true} when at least one custom effect is present
     */
    boolean hasCustomEffects();

    /**
     * Returns whether the cloud carries a custom effect of a type.
     *
     * @param type effect type to query
     * @return {@code true} when a custom effect of that type is present
     */
    boolean hasCustomEffect(EffectType type);

    /**
     * Adds a custom effect to the cloud.
     *
     * @param effect    effect to add
     * @param overwrite whether to replace an existing custom effect of the same type
     * @return {@code true} when the custom effects changed
     */
    boolean addCustomEffect(PotionEffect effect, boolean overwrite);

    /**
     * Removes a custom effect from the cloud.
     *
     * @param type effect type to remove
     * @return {@code true} when an effect was removed
     */
    boolean removeCustomEffect(EffectType type);

    /**
     * Removes every custom effect from the cloud.
     *
     * @return {@code true} when at least one effect was removed
     */
    boolean clearCustomEffects();
}
