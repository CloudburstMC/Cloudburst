package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class DestroyBlockParticle extends Particle {

    protected final int data;

    public DestroyBlockParticle(Vector3f pos, BlockState blockState) {
        super(pos);
        this.data = CloudBlockRegistry.REGISTRY.getDefinition(blockState).getRuntimeId();
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(ParticleType.ICON_CRACK);
        packet.setPosition(this.getPosition());
        packet.setData(this.data);

        return new BedrockPacket[]{packet};
    }
}
