package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Identifies translations supplied by the vanilla client.
 */
@UtilityClass
public class VanillaTranslationKeys {
    private static final String RESOURCE = "locale/vanilla/en_US.lang";
    private static final Set<String> KEYS = load();

    public boolean contains(String key) {
        return KEYS.contains(key);
    }

    private Set<String> load() {
        InputStream stream = VanillaTranslationKeys.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing vanilla translations: " + RESOURCE);
        }

        Set<String> keys = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.indexOf('=');
                if (separator > 0 && line.charAt(0) != '#') {
                    keys.add(line.substring(0, separator));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load vanilla translations", e);
        }

        return Set.copyOf(keys);
    }
}
