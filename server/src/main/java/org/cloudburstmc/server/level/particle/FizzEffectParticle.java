package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;

public final class FizzEffectParticle extends GenericParticle {

    private static final int DATA = 513;

    public FizzEffectParticle(Vector3f position) {
        super(position, LevelEvent.PARTICLE_FIZZ_EFFECT, DATA);
    }
}
