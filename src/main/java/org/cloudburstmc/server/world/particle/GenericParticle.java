package org.cloudburstmc.server.world.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.world.particle in project Nukkit .
 */
public class GenericParticle extends Particle {

    protected final LevelEventType type;
    protected final int data;

    public GenericParticle(Vector3f pos, LevelEventType type) {
        this(pos, type, 0);
    }

    public GenericParticle(Vector3f pos, LevelEventType type, int data) {
        super(pos);
        this.type = type;
        this.data = data;
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket pk = new LevelEventPacket();
        pk.setType(this.type);
        pk.setPosition(this.getPosition());
        pk.setData(this.data);

        return new BedrockPacket[]{pk};
    }
}
