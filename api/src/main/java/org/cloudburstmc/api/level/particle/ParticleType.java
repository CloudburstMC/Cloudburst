package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A registered kind of particle.
 */
public record ParticleType(Identifier id) {

    public ParticleType {
        checkNotNull(id, "id");
    }

    public static ParticleType of(Identifier id) {
        return new ParticleType(id);
    }
}
