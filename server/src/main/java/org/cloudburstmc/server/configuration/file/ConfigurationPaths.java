package org.cloudburstmc.server.configuration.file;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Escaping rules shared by configuration values and comment paths.
 */
@UtilityClass
public class ConfigurationPaths {

    public static List<String> keys(String path) {
        List<String> keys = new ArrayList<>();
        StringBuilder key = new StringBuilder();
        boolean escaped = false;

        for (char character : path.toCharArray()) {
            if (escaped) {
                key.append(character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '.') {
                keys.add(key.toString());
                key.setLength(0);
            } else {
                key.append(character);
            }
        }

        if (escaped) {
            throw new IllegalArgumentException("Unfinished path escape: " + path);
        }

        keys.add(key.toString());

        return keys;
    }

    public static int firstSeparator(String path) {
        boolean escaped = false;

        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);

            if (escaped) {
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '.') {
                return index;
            }
        }

        return -1;
    }
}
