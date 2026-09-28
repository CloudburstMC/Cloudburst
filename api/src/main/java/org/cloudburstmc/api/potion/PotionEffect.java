package org.cloudburstmc.api.potion;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * An immutable potion effect that can be applied to an entity.
 */
public class PotionEffect {

    /**
     * Duration used by effects that do not expire.
     */
    public static final int INFINITE_DURATION = -1;

    private final EffectType type;
    private final int duration;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;

    /**
     * Creates a visible, non-ambient potion effect.
     *
     * @param type      effect type
     * @param duration  duration in ticks, or {@link #INFINITE_DURATION}
     * @param amplifier zero-based effect amplifier
     */
    public PotionEffect(EffectType type, int duration, int amplifier) {
        this(type, duration, amplifier, false, true);
    }

    /**
     * Creates a potion effect.
     *
     * @param type      effect type
     * @param duration  duration in ticks, or {@link #INFINITE_DURATION}
     * @param amplifier zero-based effect amplifier
     * @param ambient   whether the effect is ambient
     * @param particles whether the effect displays particles
     */
    public PotionEffect(EffectType type, int duration, int amplifier, boolean ambient, boolean particles) {
        this.type = requireNonNull(type, "type");
        checkArgument(duration == INFINITE_DURATION || duration >= 0, "duration must be non-negative or INFINITE_DURATION");
        checkArgument(amplifier >= 0, "amplifier cannot be negative");
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
    }

    /**
     * Returns the effect type.
     *
     * @return effect type
     */
    public EffectType getType() {
        return this.type;
    }

    /**
     * Returns the remaining duration.
     *
     * @return duration in ticks, or {@link #INFINITE_DURATION}
     */
    public int getDuration() {
        return this.duration;
    }

    /**
     * Returns the zero-based effect amplifier.
     *
     * @return effect amplifier
     */
    public int getAmplifier() {
        return this.amplifier;
    }

    /**
     * Returns whether this effect is ambient.
     *
     * @return {@code true} when the effect is ambient
     */
    public boolean isAmbient() {
        return this.ambient;
    }

    /**
     * Returns whether this effect displays particles.
     *
     * @return {@code true} when particles are displayed
     */
    public boolean hasParticles() {
        return this.particles;
    }

    /**
     * Returns a copy with a different duration.
     *
     * @param duration duration in ticks, or {@link #INFINITE_DURATION}
     * @return updated potion effect
     */
    public PotionEffect withDuration(int duration) {
        return new PotionEffect(this.type, duration, this.amplifier, this.ambient, this.particles);
    }

    /**
     * Returns a copy with a different amplifier.
     *
     * @param amplifier zero-based effect amplifier
     * @return updated potion effect
     */
    public PotionEffect withAmplifier(int amplifier) {
        return new PotionEffect(this.type, this.duration, amplifier, this.ambient, this.particles);
    }

    /**
     * Returns a copy with a different ambient state.
     *
     * @param ambient whether the effect is ambient
     * @return updated potion effect
     */
    public PotionEffect withAmbient(boolean ambient) {
        return new PotionEffect(this.type, this.duration, this.amplifier, ambient, this.particles);
    }

    /**
     * Returns a copy with different particle visibility.
     *
     * @param particles whether the effect displays particles
     * @return updated potion effect
     */
    public PotionEffect withParticles(boolean particles) {
        return new PotionEffect(this.type, this.duration, this.amplifier, this.ambient, particles);
    }

    /**
     * Returns whether this effect does not expire.
     *
     * @return {@code true} when the duration is infinite
     */
    public boolean isInfinite() {
        return this.duration == INFINITE_DURATION;
    }

    /**
     * Returns whether this effect has a shorter duration than another effect.
     * Infinite effects are longer than finite effects.
     *
     * @param other effect to compare
     * @return {@code true} when this effect has the shorter duration
     */
    public boolean isShorterThan(PotionEffect other) {
        requireNonNull(other, "other");
        return !this.isInfinite() && (this.duration < other.duration || other.isInfinite());
    }

    /**
     * Returns whether this effect is harmful.
     *
     * @return {@code true} when the effect is harmful
     */
    public boolean isHarmful() {
        return this.type.getCategory() == EffectCategory.HARMFUL;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PotionEffect other)) {
            return false;
        }

        return this.duration == other.duration
                && this.amplifier == other.amplifier
                && this.ambient == other.ambient
                && this.particles == other.particles
                && this.type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.duration, this.amplifier, this.ambient, this.particles);
    }

    @Override
    public String toString() {
        return "PotionEffect{"
                + "type=" + this.type
                + ", duration=" + this.duration
                + ", amplifier=" + this.amplifier
                + ", ambient=" + this.ambient
                + ", particles=" + this.particles
                + '}';
    }
}