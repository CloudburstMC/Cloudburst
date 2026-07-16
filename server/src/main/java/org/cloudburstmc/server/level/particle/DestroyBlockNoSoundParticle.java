package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public final class DestroyBlockNoSoundParticle extends Particle {

    private final int runtimeId;

    public DestroyBlockNoSoundParticle(Vector3f position, BlockState state) {
        super(position);
        this.runtimeId = CloudBlockRegistry.REGISTRY.getRuntimeId(state);
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEvent.PARTICLE_DESTROY_BLOCK_NO_SOUND);
        packet.setPosition(this.getPosition());
        packet.setData(this.runtimeId);
        return new BedrockPacket[]{packet};
    }
}
