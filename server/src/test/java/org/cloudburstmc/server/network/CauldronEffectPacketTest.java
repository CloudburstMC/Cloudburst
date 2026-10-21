package org.cloudburstmc.server.network;

import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CauldronEffectPacketTest {

    @Test
    void dyeingUsesTheCombinedEffectWithSignedRgbData() {
        Vector3f position = Vector3f.from(0.5f, 64.9375f, 0.5f);
        LevelEventPacket packet = LevelEffectPacketFactory.cauldronInteraction(position, SoundTypes.CAULDRON_ADDDYE, 0xB02E26);

        assertSame(LevelEvent.CAULDRON_ADD_DYE, packet.getType());
        assertEquals(0xFFB02E26, packet.getData());
        assertEquals(position, packet.getPosition());
    }

    @Test
    void bucketInteractionsUseCauldronEffectsInsteadOfLooseFluidEffects() {
        assertSame(LevelEvent.CAULDRON_FILL_LAVA, LevelEffectPacketFactory.cauldronInteraction(
                Vector3f.ZERO, SoundTypes.BUCKET_EMPTY_LAVA, 0).getType());
        assertSame(LevelEvent.CAULDRON_TAKE_POWDER_SNOW, LevelEffectPacketFactory.cauldronInteraction(
                Vector3f.ZERO, SoundTypes.BUCKET_FILL_POWDER_SNOW, 0).getType());
    }

    @Test
    void fillingAndTakingPotionsUseTheirLevelEventsWithoutDyeData() {
        Vector3f position = Vector3f.from(0.5f, 64.5625f, 0.5f);
        LevelEventPacket fill = LevelEffectPacketFactory.cauldronInteraction(
                position, SoundTypes.CAULDRON_FILLPOTION, 0x123456);
        LevelEventPacket take = LevelEffectPacketFactory.cauldronInteraction(
                position, SoundTypes.CAULDRON_TAKEPOTION, 0x123456);

        assertSame(LevelEvent.CAULDRON_FILL_POTION, fill.getType());
        assertSame(LevelEvent.CAULDRON_TAKE_POTION, take.getType());
        assertEquals(0, fill.getData());
        assertEquals(0, take.getData());
        assertEquals(position, fill.getPosition());
        assertEquals(position, take.getPosition());
    }
}
