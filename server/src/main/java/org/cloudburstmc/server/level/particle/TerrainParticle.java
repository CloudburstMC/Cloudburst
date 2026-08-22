package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public final class TerrainParticle extends GenericParticle {

    public TerrainParticle(Vector3f pos, BlockState blockState) {
        super(pos, ParticleType.TERRAIN, CloudBlockRegistry.REGISTRY.getRuntimeId(blockState));
    }
}
