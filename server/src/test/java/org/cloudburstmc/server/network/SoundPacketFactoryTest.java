package org.cloudburstmc.server.network;

import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.cloudburstmc.protocol.bedrock.packet.StopSoundPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundPacketFactoryTest {

    @Test
    void usesResourcePackSoundNamesWithoutRenamingCustomNamespaces() {
        PlaySoundPacket vanilla = SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, 0.8f, 1.2f), Vector3f.ZERO);
        assertEquals("random.break", vanilla.getSound());
        assertEquals(0.8f, vanilla.getVolume());
        assertEquals(1.2f, vanilla.getPitch());
        assertEquals(-1, vanilla.getLoopCount());
        assertFalse(vanilla.isBypassListenerRangeCheck());

        SoundType custom = SoundType.of(Identifier.parse("plugin:machine.start"));
        assertEquals("plugin:machine.start", SoundPacketFactory.play(new SoundPlayback(custom, 1, 1), Vector3f.ZERO).getSound());
        assertEquals(SoundTypes.RANDOM_BREAK, SoundType.of(Identifier.parse("minecraft:random.break")));
        assertEquals("minecraft:random.break", SoundTypes.RANDOM_BREAK.key().asString());
    }

    @Test
    void distinguishesNamedStopsFromStoppingEverything() {
        StopSoundPacket named = SoundPacketFactory.stop(SoundTypes.RANDOM_BREAK);
        assertEquals("random.break", named.getSoundName());
        assertFalse(named.isStoppingAllSound());

        StopSoundPacket all = SoundPacketFactory.stopAll();
        assertEquals("", all.getSoundName());
        assertTrue(all.isStoppingAllSound());
    }

    @Test
    void rejectsInvalidPlaybackValuesBeforeSendingPackets() {
        assertThrows(IllegalArgumentException.class, () -> SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, Float.NaN, 1), Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, -1, 1), Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, 1, 0), Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, 1, Float.POSITIVE_INFINITY), Vector3f.ZERO));
        assertThrows(IllegalArgumentException.class, () -> SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, 1, 1), Vector3f.from(Float.NaN, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> SoundType.of(Identifier.EMPTY));
    }

    @Test
    void encodesRepeatDistanceAndStartOffsetSettings() {
        PlaySoundPacket packet = SoundPacketFactory.play(new SoundPlayback(SoundTypes.RANDOM_BREAK, 1, 1, 3, true, 2.5f), Vector3f.ZERO);
        assertEquals(3, packet.getLoopCount());
        assertTrue(packet.isBypassListenerRangeCheck());
        assertEquals(Float.valueOf(2.5f), packet.getPlaybackPositionSeconds());
        assertThrows(IllegalArgumentException.class, () -> new SoundPlayback(SoundTypes.RANDOM_BREAK, 1, 1, -1, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new SoundPlayback(SoundTypes.RANDOM_BREAK, 1, 1, 0, false, Float.NaN));
    }
}
