package org.cloudburstmc.api.level;

import net.kyori.adventure.translation.Translatable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * A level difficulty.
 */
public enum Difficulty implements Translatable {
    PEACEFUL(0),
    EASY(1),
    NORMAL(2),
    HARD(3);

    private final int value;

    Difficulty(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric ID of this difficulty, from 0 to 3.
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Returns the difficulty with the given numeric ID.
     *
     * @param value the numeric ID
     * @return the difficulty, or {@code null} when the ID is unknown
     */
    public static @Nullable Difficulty getByValue(int value) {
        for (Difficulty difficulty : values()) {
            if (difficulty.value == value) {
                return difficulty;
            }
        }

        return null;
    }

    @Override
    public @NonNull String translationKey() {
        return "options.difficulty." + this.name().toLowerCase(Locale.ROOT);
    }
}
