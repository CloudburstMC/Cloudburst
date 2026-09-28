package org.cloudburstmc.api.level.particle;

/**
 * A numeric emitter variable.
 *
 * @param value the finite variable value
 */
public record ParticleNumber(float value) implements ParticleVariable {

    public ParticleNumber {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Particle variable must be finite");
        }
    }
}
