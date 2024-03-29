package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

public class BlockForceFieldParticle extends GenericParticle {
    public BlockForceFieldParticle(Vector3f pos) {
        this(pos, 0);
    }

    public BlockForceFieldParticle(Vector3f pos, int scale) {
        super(pos, ParticleType.BLOCK_FORCE_FIELD);
    }
}
