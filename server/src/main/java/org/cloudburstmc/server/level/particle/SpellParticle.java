package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;

/**
 * Created on 2015/12/27 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 * The name "spell" comes from minecraft wiki.
 */
public class SpellParticle extends Particle {

    protected final int data;

    public SpellParticle(Vector3f pos) {
        this(pos, 0);
    }

    public SpellParticle(Vector3f pos, int data) {
        super(pos);
        this.data = data;
    }

    public SpellParticle(Vector3f pos, BlockColor blockColor) {
        //alpha is ignored
        this(pos, blockColor.getRed(), blockColor.getGreen(), blockColor.getBlue());
    }

    public SpellParticle(Vector3f pos, int r, int g, int b) {
        this(pos, r, g, b, 0x00);
    }

    protected SpellParticle(Vector3f pos, int r, int g, int b, int a) {
        this(pos, ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(ParticleType.WATER_SPLASH);
        packet.setPosition(getPosition());
        packet.setData(this.data);

        return new BedrockPacket[]{packet};
    }
}
