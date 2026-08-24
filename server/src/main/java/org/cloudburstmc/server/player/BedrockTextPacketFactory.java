package org.cloudburstmc.server.player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.translation.GlobalTranslator;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.cloudburstmc.server.locale.LocaleManager;

import java.util.Locale;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BedrockTextPacketFactory {
    private static final BedrockLegacyTextSerializer LEGACY_TEXT_SERIALIZER = BedrockLegacyTextSerializer.getInstance();

    public static TextPacket system(Component message) {
        return system(message, LocaleManager.FALLBACK_LOCALE);
    }

    public static TextPacket system(Component message, Locale locale) {
        TextPacket packet = base(TextPacket.Type.SYSTEM);
        packet.setNeedsTranslation(false);
        packet.setMessage(new BedrockComponent(render(message, locale)));
        return packet;
    }

    public static TextPacket message(Component message) {
        return message(message, LocaleManager.FALLBACK_LOCALE);
    }

    public static TextPacket message(Component message, Locale locale) {
        return system(message, locale);
    }

    public static TextPacket translation(Component message) {
        return translation(message, LocaleManager.FALLBACK_LOCALE);
    }

    public static TextPacket translation(Component message, Locale locale) {
        message = render(message, locale);
        if (!(message instanceof TranslatableComponent)) {
            return system(message, locale);
        }

        TextPacket packet = base(TextPacket.Type.TRANSLATION);
        if (needsSerializedMessage(packet, message)) {
            return system(message, locale);
        }
        return packet;
    }

    public static TextPacket chat(String source, Component message) {
        return chat(source, message, LocaleManager.FALLBACK_LOCALE);
    }

    public static TextPacket chat(String source, Component message, Locale locale) {
        TextPacket packet = base(TextPacket.Type.CHAT);
        packet.setSourceName(source);
        packet.setMessage(new BedrockComponent(render(message, locale)));
        return packet;
    }

    public static TextPacket popup(Component message) {
        return popup(message, LocaleManager.FALLBACK_LOCALE);
    }

    public static TextPacket popup(Component message, Locale locale) {
        message = render(message, locale);
        TextPacket packet = base(TextPacket.Type.POPUP);
        if (needsSerializedMessage(packet, message)) {
            packet.setNeedsTranslation(false);
            packet.setMessage(new BedrockComponent(message));
        }
        return packet;
    }

    public static TextPacket tip(Component message) {
        return tip(message, LocaleManager.FALLBACK_LOCALE);
    }

    public static TextPacket tip(Component message, Locale locale) {
        TextPacket packet = base(TextPacket.Type.TIP);
        packet.setMessage(new BedrockComponent(render(message, locale)));
        return packet;
    }

    private static Component render(Component message, Locale locale) {
        return GlobalTranslator.render(message, Objects.requireNonNullElse(locale, LocaleManager.FALLBACK_LOCALE));
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
