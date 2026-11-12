package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.potion.EffectType;

/**
 * A placed beacon and its configured powers.
 */
public interface Beacon extends BlockEntity {

    /**
     * Returns the number of complete pyramid layers, from zero through four.
     */
    int getTier();

    /**
     * Returns whether the pyramid is complete enough and the beam is unobstructed.
     */
    boolean isActive();

    /**
     * Returns the effect range in blocks, including any override.
     */
    double getEffectRange();

    /**
     * Overrides the effect range.
     *
     * @param range the finite, non-negative range in blocks
     */
    void setEffectRange(double range);

    /**
     * Restores the effect range determined by the pyramid tier.
     */
    void resetEffectRange();

    /**
     * Returns the selected primary power, or null if none is selected.
     */
    @Nullable
    EffectType getPrimaryEffect();

    /**
     * Returns the selected secondary power, or null if none is selected.
     */
    @Nullable
    EffectType getSecondaryEffect();

    /**
     * Sets both selected powers without taking a payment.
     *
     * @param primary   the primary power, or null to clear it
     * @param secondary the secondary power, or null to clear it
     * @throws IllegalArgumentException if either effect is unavailable to clients
     */
    void setEffects(@Nullable EffectType primary, @Nullable EffectType secondary);
}
