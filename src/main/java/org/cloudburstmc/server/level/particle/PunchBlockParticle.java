package org.cloudburstmc.server.level.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.registry.BlockRegistry;

public class PunchBlockParticle extends Particle {

    protected final int data;

    public PunchBlockParticle(Vector3f pos, BlockState state, Direction face) {
        this(pos, state.getType().getId(), BlockStateMetaMappings.getMetaFromState(state), face);
    }

    public PunchBlockParticle(Vector3f pos, Identifier blockId, int blockDamage, Direction face) {
        super(pos);
        this.data = BlockRegistry.get().getRuntimeId(blockId, blockDamage) | (face.getIndex() << 24);
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEventType.PARTICLE_CRACK_BLOCK);
        packet.setPosition(this.getPosition());
        packet.setData(this.data);

        return new BedrockPacket[]{packet};
    }
}
