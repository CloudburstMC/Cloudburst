package org.cloudburstmc.server.command;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.cloudburstmc.server.locale.LocaleManager;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Objects;

/**
 * Renders components for the plain-text server console.
 */
@UtilityClass
public class CloudConsoleMessageRenderer {
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

    public Component render(Component message, LocaleManager localeManager) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(localeManager, "localeManager");

        if (!(message instanceof TranslatableComponent translation)) {
            return GlobalTranslator.render(message, locale(localeManager));
        }

        String pattern = localeManager.get(translation.key());
        if (pattern == null) {
            pattern = translation.fallback();
        }

        if (pattern == null) {
            return GlobalTranslator.render(message, locale(localeManager));
        }

        Object[] arguments = translation.arguments().stream()
                .map(argument -> renderArgument(argument, localeManager))
                .toArray();
        try {
            Component rendered = Component.text(String.format(locale(localeManager), pattern, arguments))
                    .style(translation.style());
            for (Component child : translation.children()) {
                rendered = rendered.append(render(child, localeManager));
            }

            return rendered;
        } catch (IllegalFormatException ignored) {
            return GlobalTranslator.render(message, locale(localeManager));
        }
    }

    private Object renderArgument(TranslationArgument argument, LocaleManager localeManager) {
        Object value = argument.value();
        if (value instanceof Component component) {
            return PLAIN_TEXT_SERIALIZER.serialize(render(component, localeManager));
        }

        return value;
    }

    private Locale locale(LocaleManager localeManager) {
        return Objects.requireNonNullElse(localeManager.getLocale(), LocaleManager.FALLBACK_LOCALE);
    }
}
