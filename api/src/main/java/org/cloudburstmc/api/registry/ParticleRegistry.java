package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.util.Identifier;

/**
 * Registry for particle types.
 */
public interface ParticleRegistry extends KeyedRegistry<ParticleType> {

    @Override
    default Identifier getId(ParticleType value) {
        return value.id();
    }
}
