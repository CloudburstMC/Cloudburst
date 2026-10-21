package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.BlockPalette;

import java.util.Map;

/**
 * Encodes gameplay effects that may combine particles, sounds, and animations.
 */
@UtilityClass
public class LevelEffectPacketFactory {

    private static final Map<SoundType, LevelEvent> CAULDRON_INTERACTIONS = Map.ofEntries(
            Map.entry(SoundTypes.CAULDRON_ADDDYE, LevelEvent.CAULDRON_ADD_DYE),
            Map.entry(SoundTypes.CAULDRON_CLEANARMOR, LevelEvent.CAULDRON_CLEAN_ARMOR),
            Map.entry(SoundTypes.CAULDRON_CLEANBANNER, LevelEvent.CAULDRON_CLEAN_BANNER),
            Map.entry(SoundTypes.CAULDRON_DYEARMOR, LevelEvent.CAULDRON_DYE_ARMOR),
            Map.entry(SoundTypes.CAULDRON_EXPLODE, LevelEvent.CAULDRON_EXPLODE),
            Map.entry(SoundTypes.CAULDRON_FILLPOTION, LevelEvent.CAULDRON_FILL_POTION),
            Map.entry(SoundTypes.CAULDRON_TAKEPOTION, LevelEvent.CAULDRON_TAKE_POTION),
            Map.entry(SoundTypes.CAULDRON_FILLWATER, LevelEvent.CAULDRON_FILL_WATER),
            Map.entry(SoundTypes.CAULDRON_TAKEWATER, LevelEvent.CAULDRON_TAKE_WATER),
            Map.entry(SoundTypes.BUCKET_EMPTY_LAVA, LevelEvent.CAULDRON_FILL_LAVA),
            Map.entry(SoundTypes.BUCKET_FILL_LAVA, LevelEvent.CAULDRON_TAKE_LAVA),
            Map.entry(SoundTypes.BUCKET_EMPTY_POWDER_SNOW, LevelEvent.CAULDRON_FILL_POWDER_SNOW),
            Map.entry(SoundTypes.BUCKET_FILL_POWDER_SNOW, LevelEvent.CAULDRON_TAKE_POWDER_SNOW));

    public static LevelEventPacket cauldronInteraction(Vector3f position, SoundType sound, int color) {
        LevelEvent type = CAULDRON_INTERACTIONS.get(sound);
        if (type == null) {
            throw new IllegalArgumentException("Unknown cauldron interaction: " + sound);
        }

        int data = type == LevelEvent.CAULDRON_ADD_DYE ? color << 8 >> 8 : 0;
        return event(position, type, data);
    }

    public static LevelEventPacket blockDestruction(Vector3f position, BlockState state, boolean sound) {
        return event(position, sound ? LevelEvent.PARTICLE_DESTROY_BLOCK : LevelEvent.PARTICLE_DESTROY_BLOCK_NO_SOUND,
                BlockPalette.INSTANCE.getDefinition(state).getRuntimeId());
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

        return event(position, type, BlockPalette.INSTANCE.getDefinition(state).getRuntimeId());
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
