package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public final class DestroyBlockParticle extends Particle {

    private final int runtimeId;

    public DestroyBlockParticle(Vector3f pos, BlockState blockState) {
        super(pos);
        this.runtimeId = CloudBlockRegistry.REGISTRY.getRuntimeId(blockState);
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
        packet.setPosition(this.getPosition());
        packet.setData(this.runtimeId);

        return new BedrockPacket[]{packet};
    }
}
