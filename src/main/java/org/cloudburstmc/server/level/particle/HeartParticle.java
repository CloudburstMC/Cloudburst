package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class HeartParticle extends GenericParticle {
    public HeartParticle(Vector3f pos) {
        this(pos, 0);
    }

    public HeartParticle(Vector3f pos, int scale) {
        super(pos, ParticleType.HEART, scale);
    }
}
