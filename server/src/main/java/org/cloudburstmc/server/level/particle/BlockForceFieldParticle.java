package org.cloudburstmc.server.level.particle;

import com.nukkitx.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.math.vector.Vector3f;

public class BlockForceFieldParticle extends GenericParticle {
    public BlockForceFieldParticle(Vector3f pos) {
        this(pos, 0);
    }

    public BlockForceFieldParticle(Vector3f pos, int scale) {
        super(pos, LevelEventType.PARTICLE_BLOCK_FORCE_FIELD);
    }
}
