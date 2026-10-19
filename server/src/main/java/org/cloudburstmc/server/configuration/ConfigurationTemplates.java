package org.cloudburstmc.server.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.configuration.validation.ConfigurationProblem;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.persistence.AtomicFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders bundled server settings with localized comments before creating a file.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigurationTemplates {
    private static final String FALLBACK_LANGUAGE = "en_US";
    private static final Pattern KEY = Pattern.compile("%([A-Za-z0-9._-]+)");

    public static void ensure(Path file, String resource, String language, boolean create) throws IOException {
        if (Files.exists(file)) {
            return;
        }

        if (!create) {
            throw new ConfigurationValidationException(file, List.of(new ConfigurationProblem("<document>", null, "must exist before reloading")));
        }

        AtomicFiles.write(file, render(resource, language));
    }

    public static String render(String resource, String language) throws IOException {
        String[] parts = language.split("[_-]", -1);
        if (parts.length != 2 || !parts[0].matches("[A-Za-z]{2,3}") || !parts[1].matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Invalid configuration language: " + language);
        }

        String locale = Locale.of(parts[0], parts[1]).toString();
        Map<String, String> translations = new HashMap<>();
        loadTranslations(FALLBACK_LANGUAGE, translations, true);
        if (!locale.equals(FALLBACK_LANGUAGE)) {
            loadTranslations(locale, translations, false);
        }
        translations.put("language", locale);

        String template;
        try (InputStream stream = ConfigurationTemplates.class.getResourceAsStream("/" + resource)) {
            if (stream == null) {
                throw new IOException("Missing bundled configuration: " + resource);
            }
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        Matcher matcher = KEY.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String value = translations.get(matcher.group(1));
            if (value == null) {
                int lineStart = template.lastIndexOf('\n', matcher.start()) + 1;
                String prefix = template.substring(lineStart, matcher.start()).stripLeading();
                if (!prefix.startsWith("#")) {
                    throw new IOException("Missing configuration value translation: " + matcher.group(1) + " in " + resource);
                }

                value = matcher.group(1);
            }

            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static void loadTranslations(String language, Map<String, String> translations, boolean required) throws IOException {
        String resource = "/locale/cloudburst/configs/" + language + ".lang";
        try (InputStream stream = ConfigurationTemplates.class.getResourceAsStream(resource)) {
            if (stream == null) {
                if (required) {
                    throw new IOException("Missing configuration translations: " + resource);
                }

                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int separator = line.indexOf('=');
                    if (!line.startsWith("#") && separator > 0) {
                        translations.put(line.substring(0, separator), line.substring(separator + 1));
                    }
                }
            }
        }
    }
}
