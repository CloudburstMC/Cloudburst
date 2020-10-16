package org.cloudburstmc.server.level.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

public class PunchBlockParticle extends Particle {

    protected final int data;

    public PunchBlockParticle(Vector3f pos, BlockState state, Direction face) {
        this(pos, state.getId(), BlockStateMetaMappings.getMetaFromState(state), face);
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
