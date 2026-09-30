package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.cloudburstmc.protocol.bedrock.packet.StopSoundPacket;

import java.util.Objects;

/**
 * Encodes named sound playback and stop requests.
 */
@UtilityClass
public class SoundPacketFactory {

    public static PlaySoundPacket play(SoundPlayback playback, Vector3f position) {
        Objects.requireNonNull(playback, "playback");
        Objects.requireNonNull(position, "position");
        if (!Float.isFinite(position.getX()) || !Float.isFinite(position.getY()) || !Float.isFinite(position.getZ())) {
            throw new IllegalArgumentException("Sound position must be finite");
        }

        PlaySoundPacket packet = new PlaySoundPacket();
        packet.setSound(soundName(playback.sound()));
        packet.setPosition(position);
        packet.setVolume(playback.volume());
        packet.setPitch(playback.pitch());
        packet.setLoopCount(playback.repeatCount());
        packet.setBypassListenerRangeCheck(playback.ignoreDistance());
        if (playback.startOffset() > 0) {
            packet.setPlaybackPositionSeconds(playback.startOffset());
        }

        return packet;
    }

    public static StopSoundPacket stop(SoundType sound) {
        StopSoundPacket packet = new StopSoundPacket();
        packet.setSoundName(soundName(sound));
        return packet;
    }

    public static StopSoundPacket stopAll() {
        StopSoundPacket packet = new StopSoundPacket();
        packet.setSoundName("");
        packet.setStoppingAllSound(true);
        return packet;
    }

    private static String soundName(SoundType sound) {
        Identifier id = Objects.requireNonNull(sound, "sound").getId();
        return "minecraft".equals(id.getNamespace()) ? id.getName() : id.toString();
    }
}
