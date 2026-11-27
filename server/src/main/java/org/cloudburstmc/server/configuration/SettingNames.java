package org.cloudburstmc.server.configuration;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * Lowercase names used for enum values in settings and commands, such as {@code survival} or {@code easy}.
 */
@UtilityClass
public class SettingNames {

    public static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the constant whose lowercase name equals the given name, or null when none does.
     */
    public static <E extends Enum<E>> @Nullable E byName(Class<E> type, String name) {
        for (E value : type.getEnumConstants()) {
            if (name(value).equals(name)) {
                return value;
            }
        }

        return null;
    }
}
