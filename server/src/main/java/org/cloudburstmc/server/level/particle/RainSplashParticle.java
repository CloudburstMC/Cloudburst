package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

public class RainSplashParticle extends GenericParticle {
    public RainSplashParticle(Vector3f pos) {
        super(pos, ParticleType.RAIN_SPLASH);
    }
}
