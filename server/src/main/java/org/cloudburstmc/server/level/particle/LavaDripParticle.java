package org.cloudburstmc.server.level.particle;

import com.nukkitx.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class LavaDripParticle extends GenericParticle {
    public LavaDripParticle(Vector3f pos) {
        super(pos, LevelEventType.PARTICLE_DRIP_LAVA);
    }
}
