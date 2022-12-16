package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class CriticalParticle extends GenericParticle {
    public CriticalParticle(Vector3f pos) {
        this(pos, 2);
    }

    public CriticalParticle(Vector3f pos, int scale) {
        super(pos, ParticleType.CRIT, scale);
    }
}
