package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
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
