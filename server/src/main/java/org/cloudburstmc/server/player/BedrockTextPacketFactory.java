package org.cloudburstmc.server.player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedrockTextPacketFactory {
    private static final BedrockLegacyTextSerializer LEGACY_TEXT_SERIALIZER = BedrockLegacyTextSerializer.getInstance();

    public static TextPacket system(Component message) {
        TextPacket packet = base(TextPacket.Type.SYSTEM);
        packet.setNeedsTranslation(false);
        packet.setMessage(new BedrockComponent(message));
        return packet;
    }

    public static TextPacket message(Component message) {
        return message instanceof TranslatableComponent ? translation(message) : system(message);
    }

    public static TextPacket translation(Component message) {
        TextPacket packet = base(TextPacket.Type.TRANSLATION);
        if (needsSerializedMessage(packet, message)) {
            return system(message);
        }
        return packet;
    }

    public static TextPacket chat(String source, Component message) {
        TextPacket packet = base(TextPacket.Type.CHAT);
        packet.setSourceName(source);
        packet.setMessage(new BedrockComponent(message));
        return packet;
    }

    public static TextPacket popup(Component message) {
        TextPacket packet = base(TextPacket.Type.POPUP);
        if (needsSerializedMessage(packet, message)) {
            packet.setNeedsTranslation(false);
            packet.setMessage(new BedrockComponent(message));
        }
        return packet;
    }

    public static TextPacket tip(Component message) {
        TextPacket packet = base(TextPacket.Type.TIP);
        packet.setMessage(new BedrockComponent(message));
        return packet;
    }

    private static TextPacket base(TextPacket.Type type) {
        TextPacket packet = new TextPacket();
        packet.setType(type);
        packet.setPlatformChatId("");
        packet.setSourceName("");
        packet.setXuid("");
        return packet;
    }

    private static boolean needsSerializedMessage(TextPacket packet, Component message) {
        if (message instanceof TranslatableComponent translatableComponent && translatableComponent.children().isEmpty()) {
            packet.setNeedsTranslation(true);
            packet.setMessage(translatableComponent.style().isEmpty() ? translatableComponent.key() : LEGACY_TEXT_SERIALIZER.serialize(message));
            packet.getParameters().clear();
            for (TranslationArgument argument : translatableComponent.arguments()) {
                packet.getParameters().add(serializeTranslationArgument(argument));
            }
            return false;
        }

        return true;
    }

    private static String serializeTranslationArgument(TranslationArgument argument) {
        Object value = argument.value();
        if (value instanceof Component component) {
            return LEGACY_TEXT_SERIALIZER.serialize(component);
        }
        return Objects.toString(value, "");
    }
}
