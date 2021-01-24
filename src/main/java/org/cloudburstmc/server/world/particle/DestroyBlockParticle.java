package org.cloudburstmc.server.world.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.world.particle in project Nukkit .
 */
public class DestroyBlockParticle extends Particle {

    protected final int data;

    public DestroyBlockParticle(Vector3f pos, BlockState blockState) {
        super(pos);
        this.data = BlockRegistry.get().getRuntimeId(blockState);
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
        packet.setPosition(this.getPosition());
        packet.setData(this.data);

        return new BedrockPacket[]{packet};
    }
}
