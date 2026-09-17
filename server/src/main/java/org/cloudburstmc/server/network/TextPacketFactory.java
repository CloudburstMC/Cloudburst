package org.cloudburstmc.server.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.translation.GlobalTranslator;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;

import java.util.Locale;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Creates text packets for messages sent to players.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextPacketFactory {
    private static final int MAX_TRANSLATION_PARAMETERS = 4;
    private static final BedrockLegacyTextSerializer LEGACY_TEXT_SERIALIZER = BedrockLegacyTextSerializer.getInstance();

    /**
     * Creates a packet for a system message.
     *
     * @param message the message
     * @param locale  the recipient locale
     * @return the text packet
     */
    public static TextPacket system(Component message, Locale locale) {
        requireNonNull(message, "message");
        requireNonNull(locale, "locale");

        if (message instanceof TranslatableComponent translation && canTranslateClientSide(translation)) {
            return translation(translation, locale);
        }

        return localized(TextPacket.Type.SYSTEM, message, locale);
    }

    /**
     * Creates a packet for a chat message attributed to a source.
     *
     * @param source  the displayed source name
     * @param message the message
     * @param locale  the recipient locale
     * @return the text packet
     */
    public static TextPacket chat(String source, Component message, Locale locale) {
        TextPacket packet = localized(TextPacket.Type.CHAT, message, locale);
        packet.setSourceName(requireNonNull(source, "source"));
        return packet;
    }

    /**
     * Creates a packet for a popup message.
     *
     * @param message the message
     * @param locale  the recipient locale
     * @return the text packet
     */
    public static TextPacket popup(Component message, Locale locale) {
        return localized(TextPacket.Type.POPUP, message, locale);
    }

    /**
     * Creates a packet for a tip message.
     *
     * @param message the message
     * @param locale  the recipient locale
     * @return the text packet
     */
    public static TextPacket tip(Component message, Locale locale) {
        return localized(TextPacket.Type.TIP, message, locale);
    }

    private static TextPacket localized(TextPacket.Type type, Component message, Locale locale) {
        TextPacket packet = base(type);
        packet.setNeedsTranslation(false);
        packet.setMessage(new BedrockComponent(GlobalTranslator.render(
                requireNonNull(message, "message"),
                requireNonNull(locale, "locale")
        )));
        return packet;
    }

    private static boolean canTranslateClientSide(TranslatableComponent translation) {
        return translation.children().isEmpty()
                && translation.arguments().size() <= MAX_TRANSLATION_PARAMETERS
                && VanillaTranslationKeys.contains(translation.key());
    }

    private static TextPacket translation(TranslatableComponent message, Locale locale) {
        TextPacket packet = base(TextPacket.Type.TRANSLATION);
        packet.setNeedsTranslation(true);
        packet.setMessage(message.style().isEmpty() ? message.key() : LEGACY_TEXT_SERIALIZER.serialize(message));
        for (TranslationArgument argument : message.arguments()) {
            packet.getParameters().add(serializeTranslationArgument(argument, locale));
        }

        return packet;
    }

    private static String serializeTranslationArgument(TranslationArgument argument, Locale locale) {
        Object value = argument.value();
        if (value instanceof Component component) {
            Component serialized = component instanceof TranslatableComponent translation
                    && canSerializeTranslationArgument(translation) ? component
                    : GlobalTranslator.render(component, locale);
            return LEGACY_TEXT_SERIALIZER.serialize(serialized);
        }

        return Objects.toString(value, "");
    }

    private static boolean canSerializeTranslationArgument(TranslatableComponent translation) {
        return translation.children().isEmpty()
                && translation.arguments().isEmpty()
                && VanillaTranslationKeys.contains(translation.key());
    }

    private static TextPacket base(TextPacket.Type type) {
        TextPacket packet = new TextPacket();
        packet.setType(requireNonNull(type, "type"));
        packet.setPlatformChatId("");
        packet.setSourceName("");
        packet.setXuid("");
        return packet;
    }
}
