package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class TerrainParticle extends GenericParticle {
    public TerrainParticle(Vector3f pos, BlockState blockState) {
        super(pos, LevelEventType.PARTICLE_TERRAIN, CloudBlockRegistry.REGISTRY.getRuntimeId(blockState));
    }
}
