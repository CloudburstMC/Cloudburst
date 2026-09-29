package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.math.vector.Vector3f;

import java.util.Objects;

/**
 * Describes a group of particles distributed around an origin.
 *
 * @param options the particle and its rendering values
 * @param count   the positive number of particles
 * @param spread  the finite, non-negative standard deviation on each axis
 */
public record ParticleEmission(ParticleOptions options, int count, Vector3f spread) {

    public ParticleEmission {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(spread, "spread");

        if (count < 1) {
            throw new IllegalArgumentException("Particle count must be positive");
        }

        if (!Float.isFinite(spread.getX()) || !Float.isFinite(spread.getY()) || !Float.isFinite(spread.getZ())
                || spread.getX() < 0 || spread.getY() < 0 || spread.getZ() < 0) {
            throw new IllegalArgumentException("Particle spread must be finite and non-negative");
        }
    }

    /**
     * Creates one particle with no positional spread.
     *
     * @param options the particle and its rendering values
     */
    public ParticleEmission(ParticleOptions options) {
        this(options, 1, Vector3f.ZERO);
    }
}
