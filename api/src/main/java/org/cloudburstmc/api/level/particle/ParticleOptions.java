package org.cloudburstmc.api.level.particle;

/**
 * Describes a built-in particle and any values needed to render it.
 * A {@link ParticleType} can be used directly when no additional values are needed.
 */
public interface ParticleOptions {

    /**
     * Returns the particle being rendered.
     *
     * @return the particle type
     */
    ParticleType getType();
}
