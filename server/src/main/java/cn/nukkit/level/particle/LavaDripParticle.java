package cn.nukkit.level.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class LavaDripParticle extends GenericParticle {
    public LavaDripParticle(Vector3f pos) {
        super(pos, LevelEventType.PARTICLE_DRIP_LAVA);
    }
}
