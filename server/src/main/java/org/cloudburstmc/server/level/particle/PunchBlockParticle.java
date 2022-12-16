package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class PunchBlockParticle extends Particle {

    protected final int data;

    public PunchBlockParticle(Vector3f pos, BlockState state, Direction face) {
        this(pos, state.getType().getId(), BlockPalette.INSTANCE.getRuntimeId(state), face);
    }

    public PunchBlockParticle(Vector3f pos, Identifier blockId, int blockDamage, Direction face) {
        super(pos);
        this.data = CloudBlockRegistry.REGISTRY.getRuntimeId(blockId, blockDamage) | (face.getIndex() << 24);
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
