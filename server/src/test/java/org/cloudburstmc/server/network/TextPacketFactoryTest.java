package org.cloudburstmc.server.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.adventure.AdventureTextConverter;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

class TextPacketFactoryTest {

    @Test
    void incomingAdventureChatPreservesLiteralFormattingTags() {
        String message = "<red>literal</red>";
        BedrockCodec codec = ProtocolInfo.getDefaultPacketCodec();
        BedrockCodecHelper helper = codec.createHelper();
        helper.setTextConverter(new AdventureTextConverter());
        TextPacket outgoing = TextPacketFactory.chat("Player", Component.text(message), Locale.US);
        ByteBuf buffer = Unpooled.buffer();

        try {
            codec.tryEncode(helper, buffer, outgoing);
            TextPacket incoming = assertInstanceOf(TextPacket.class, codec.tryDecode(helper, buffer, requireNonNull(codec.getPacketDefinition(TextPacket.class)).getId()));

            assertEquals(TextPacket.Type.CHAT, incoming.getType());
            assertInstanceOf(BedrockComponent.class, incoming.getMessage(CharSequence.class));
            assertEquals(message, incoming.getMessage(CharSequence.class).toString());
        } finally {
            buffer.release();
        }
    }

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
