package org.cloudburstmc.server.network;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class TextPacketFactoryTest {

    @Test
    public void delegatesVanillaTranslationsToTheClient() {
        TextPacket packet = TextPacketFactory.system(Component.translatable(
                "commands.generic.unknown",
                Component.text("missing")
        ).color(NamedTextColor.RED), Locale.US);

        assertEquals(TextPacket.Type.TRANSLATION, packet.getType());
        assertTrue(packet.isNeedsTranslation());
        assertTrue(packet.getMessage().contains("%commands.generic.unknown"));
        assertEquals(List.of("missing"), packet.getParameters());
    }

    @Test
    public void rendersServerTranslationsBeforeSending() {
        TextPacket packet = TextPacketFactory.system(
                Component.translatable("cloudburst.test.message"),
                Locale.US
        );

        assertEquals(TextPacket.Type.SYSTEM, packet.getType());
        assertFalse(packet.isNeedsTranslation());
        assertInstanceOf(BedrockComponent.class, packet.getMessage(CharSequence.class));
    }
}
