package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class PortalParticle extends GenericParticle {
    public PortalParticle(Vector3f pos) {
        super(pos, ParticleType.PORTAL);
    }
}
