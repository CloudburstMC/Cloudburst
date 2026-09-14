package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;

public class BoneMealParticle extends Particle {

    private static final int PARTICLE_COUNT = 15;

    public BoneMealParticle(Vector3i pos) {
        super(pos.toFloat().add(0.5, 0.5, 0.5));
    }

    public BoneMealParticle(Vector3f pos) {
        super(pos);
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEvent.PARTICLE_CROP_GROWTH);
        packet.setPosition(this.getPosition());
        packet.setData(PARTICLE_COUNT);

        return new BedrockPacket[]{packet};
    }
}
