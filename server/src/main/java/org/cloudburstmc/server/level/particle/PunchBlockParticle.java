package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public final class PunchBlockParticle extends Particle {

    private final LevelEventType type;
    private final int runtimeId;

    public PunchBlockParticle(Vector3f pos, BlockState state, Direction face) {
        this(pos, CloudBlockRegistry.REGISTRY.getRuntimeId(state), face);
    }

    private PunchBlockParticle(Vector3f pos, int runtimeId, Direction face) {
        super(pos);
        this.type = particleType(face);
        this.runtimeId = runtimeId;
    }

    @Override
    public BedrockPacket[] encode() {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(this.type);
        packet.setPosition(this.getPosition());
        packet.setData(this.runtimeId);

        return new BedrockPacket[]{packet};
    }

    private static LevelEventType particleType(Direction face) {
        return switch (face) {
            case DOWN -> LevelEvent.PARTICLE_BREAK_BLOCK_DOWN;
            case UP -> LevelEvent.PARTICLE_BREAK_BLOCK_UP;
            case NORTH -> LevelEvent.PARTICLE_BREAK_BLOCK_NORTH;
            case SOUTH -> LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH;
            case WEST -> LevelEvent.PARTICLE_BREAK_BLOCK_WEST;
            case EAST -> LevelEvent.PARTICLE_BREAK_BLOCK_EAST;
        };
    }
}
