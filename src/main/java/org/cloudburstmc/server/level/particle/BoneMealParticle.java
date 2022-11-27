package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;

/**
 * Created by CreeperFace on 15.4.2017.
 */
public class BoneMealParticle extends Particle {

    private Vector3f position;

    public BoneMealParticle(Vector3i pos) {
        super(pos.toFloat().add(0.5, 0.5, 0.5));
    }

    public BoneMealParticle(Vector3f pos) {
        super(pos);
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEventType.PARTICLE_CROP_GROWTH);
        packet.setPosition(this.getPosition());

        return new BedrockPacket[]{packet};
    }
}
