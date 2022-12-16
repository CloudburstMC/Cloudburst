package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class RedstoneParticle extends GenericParticle {
    public RedstoneParticle(Vector3f pos) {
        this(pos, 1);
    }

    public RedstoneParticle(Vector3f pos, int lifetime) {
        super(pos, ParticleType.RED_DUST, lifetime);
    }
}
