package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * Encodes gameplay effects that may combine particles, sounds, and animations.
 */
@UtilityClass
public class LevelEffectPacketFactory {
    public static LevelEventPacket blockDestruction(Vector3f position, BlockState state, boolean sound) {
        return event(position, sound ? LevelEvent.PARTICLE_DESTROY_BLOCK : LevelEvent.PARTICLE_DESTROY_BLOCK_NO_SOUND,
                CloudBlockRegistry.REGISTRY.getRuntimeId(state));
    }

    public static LevelEventPacket blockPunch(Vector3f position, BlockState state, Direction face) {
        LevelEvent type = switch (face) {
            case DOWN -> LevelEvent.PARTICLE_BREAK_BLOCK_DOWN;
            case UP -> LevelEvent.PARTICLE_BREAK_BLOCK_UP;
            case NORTH -> LevelEvent.PARTICLE_BREAK_BLOCK_NORTH;
            case SOUTH -> LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH;
            case WEST -> LevelEvent.PARTICLE_BREAK_BLOCK_WEST;
            case EAST -> LevelEvent.PARTICLE_BREAK_BLOCK_EAST;
        };

        return event(position, type, CloudBlockRegistry.REGISTRY.getRuntimeId(state));
    }

    public static LevelEventPacket dragonEggTeleport(Vector3i origin, Vector3i destination) {
        Vector3i difference = origin.sub(destination);
        int data = Math.abs(difference.getX()) << 16 | Math.abs(difference.getY()) << 8 | Math.abs(difference.getZ());
        if (difference.getX() < 0) {
            data |= 1 << 24;
        }

        if (difference.getY() < 0) {
            data |= 1 << 25;
        }

        if (difference.getZ() < 0) {
            data |= 1 << 26;
        }

        return event(origin.toFloat(), LevelEvent.PARTICLE_DRAGON_EGG, data);
    }

    public static LevelEventPacket cropGrowth(Vector3i position) {
        return event(position.toFloat().add(0.5f, 0.5f, 0.5f), LevelEvent.PARTICLE_CROP_GROWTH, 15);
    }

    public static LevelEventPacket fizz(Vector3f position) {
        return event(position, LevelEvent.PARTICLE_FIZZ_EFFECT, 513);
    }

    public static LevelEventPacket event(Vector3f position, LevelEventType type, int data) {
        ParticlePacketFactory.validatePosition(position);
        LevelEventPacket packet = new LevelEventPacket();
        packet.setPosition(position);
        packet.setType(type);
        packet.setData(data);
        return packet;
    }
}
