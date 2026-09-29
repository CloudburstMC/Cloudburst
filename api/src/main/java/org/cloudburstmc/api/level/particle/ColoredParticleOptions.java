package org.cloudburstmc.api.level.particle;

import java.awt.*;
import java.util.Objects;

/**
 * Supplies a color to a particle that supports tinting.
 *
 * @param type  the colorable particle type
 * @param color the particle color, including alpha
 */
public record ColoredParticleOptions(ParticleType type, Color color) implements ParticleOptions {

    public ColoredParticleOptions {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(color, "color");
    }

    @Override
    public ParticleType getType() {
        return this.type;
    }
}
