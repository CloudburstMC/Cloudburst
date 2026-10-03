package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.block.LiquidType;
import org.cloudburstmc.api.block.LiquidTypes;

import java.util.List;

/**
 * Flotation settings for an entity type.
 *
 * @param applyGravity         whether gravity contributes to motion
 * @param baseBuoyancy         flotation strength
 * @param largeWaveProbability probability of a large wave
 * @param largeWaveSpeed       speed of large waves
 * @param dragOnRemoval        downward drag when buoyancy is removed
 * @param simulateWaves        whether wave motion is enabled
 * @param canStepFromLiquid    whether the entity can step out of a liquid onto land
 * @param liquids              liquid types in which the entity floats
 */
public record Buoyancy(boolean applyGravity, float baseBuoyancy, float largeWaveProbability,
                       float largeWaveSpeed, float dragOnRemoval, boolean simulateWaves, boolean canStepFromLiquid,
                       List<LiquidType> liquids) {

    /**
     * Creates flotation settings with an immutable liquid list.
     *
     * @throws IllegalArgumentException if a numeric value is not finite, the wave probability
     *                                  is outside {@code [0, 1]}, or the liquid list is empty or contains
     *                                  {@link LiquidTypes#EMPTY}
     * @throws NullPointerException     if the liquid list or one of its entries is null
     */
    public Buoyancy {
        if (!Float.isFinite(baseBuoyancy) || !Float.isFinite(largeWaveProbability) || !Float.isFinite(largeWaveSpeed) || !Float.isFinite(dragOnRemoval)) {
            throw new IllegalArgumentException("Buoyancy values must be finite");
        }

        if (largeWaveProbability < 0 || largeWaveProbability > 1) {
            throw new IllegalArgumentException("Large wave probability must be between zero and one");
        }

        liquids = List.copyOf(liquids);
        if (liquids.isEmpty() || liquids.contains(LiquidTypes.EMPTY)) {
            throw new IllegalArgumentException("Buoyancy requires non-empty liquid types");
        }
    }
}
