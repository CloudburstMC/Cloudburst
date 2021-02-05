package org.cloudburstmc.server.level.particle;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class Particle {

    private Vector3f position;

    public Particle() {
        this.position = Vector3f.ZERO;
    }

    public Particle(Vector3f position) {
        this.position = position;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    abstract public BedrockPacket[] encode();
}
