package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.util.Identifier;

/**
 * Provides the particle types known to the server.
 */
public interface ParticleRegistry extends KeyedRegistry<ParticleType> {

    @Override
    default Identifier getId(ParticleType value) {
        return value.getId();
    }
}
