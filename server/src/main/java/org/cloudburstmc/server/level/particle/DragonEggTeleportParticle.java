package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;

public class DragonEggTeleportParticle extends Particle {

    private final Vector3i difference;

    public DragonEggTeleportParticle(Vector3i position, Vector3i destination) {
        super(position.toFloat());
        this.difference = position.sub(destination);
    }

    @Override
    public BedrockPacket[] encode() {
        int data = Math.abs(this.difference.getX()) << 16 | Math.abs(this.difference.getY()) << 8 | Math.abs(this.difference.getZ());
        if (this.difference.getX() < 0) {
            data |= 1 << 24;
        }

        if (this.difference.getY() < 0) {
            data |= 1 << 25;
        }

        if (this.difference.getZ() < 0) {
            data |= 1 << 26;
        }

        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEvent.PARTICLE_DRAGON_EGG);
        packet.setPosition(this.getPosition());
        packet.setData(data);
        return new BedrockPacket[]{packet};
    }
}
