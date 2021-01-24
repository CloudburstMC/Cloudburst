package org.cloudburstmc.server.world.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.world.particle in project Nukkit .
 */
public class EnchantParticle extends GenericParticle {
    public EnchantParticle(Vector3f pos) {
        super(pos, LevelEventType.PARTICLE_MOB_SPELL);
    }
}
