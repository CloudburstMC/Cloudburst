package org.cloudburstmc.api.level.particle;

import java.util.Objects;

/**
 * Adjusts the scale of smoke, critical-hit, heart, or ink particles.
 *
 * @param type  the particle type
 * @param scale a non-negative scale
 */
public record ScaledParticleOptions(ParticleType type, int scale) implements ParticleOptions {

    public ScaledParticleOptions {
        Objects.requireNonNull(type, "type");
        if (scale < 0) {
            throw new IllegalArgumentException("Particle scale must not be negative");
        }
    }

    @Override
    public ParticleType getType() {
        return this.type;
    }
}
