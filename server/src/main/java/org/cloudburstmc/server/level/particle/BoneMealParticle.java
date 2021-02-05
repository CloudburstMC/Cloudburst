package org.cloudburstmc.server.level.particle;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

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
