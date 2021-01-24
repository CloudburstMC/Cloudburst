package org.cloudburstmc.server.world.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.world.particle in project Nukkit .
 */
public class RedstoneParticle extends GenericParticle {
    public RedstoneParticle(Vector3f pos) {
        this(pos, 1);
    }

    public RedstoneParticle(Vector3f pos, int lifetime) {
        super(pos, LevelEventType.PARTICLE_REDSTONE, lifetime);
    }
}
