package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

public class SplashParticle extends GenericParticle {
    public SplashParticle(Vector3f pos) {
        super(pos, ParticleType.WATER_SPLASH);
    }
}
