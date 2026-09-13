package org.cloudburstmc.api.level;

import net.kyori.adventure.translation.Translatable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A level difficulty.
 */
public enum Difficulty implements Translatable {
    PEACEFUL(0, "peaceful", "p"),
    EASY(1, "easy", "e"),
    NORMAL(2, "normal", "n"),
    HARD(3, "hard", "h");

    private final int id;
    private final Set<String> aliases;

    Difficulty(int id, String... aliases) {
        this.id = id;
        this.aliases = new HashSet<>(Arrays.asList(aliases));
        this.aliases.add(Integer.toString(id));
    }

    /**
     * Returns the stable numeric ID of this difficulty.
     *
     * @return the difficulty ID
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the difficulty represented by a numeric ID.
     *
     * @param id the difficulty ID
     * @return the corresponding difficulty
     * @throws IllegalArgumentException if the ID is unknown
     */
    public static Difficulty fromId(int id) {
        for (Difficulty difficulty : values()) {
            if (difficulty.id == id) {
                return difficulty;
            }
        }

        throw new IllegalArgumentException("Unknown difficulty ID: " + id);
    }

    /**
     * Finds a difficulty by name, alias, or numeric ID.
     *
     * @param input the value to parse
     * @return the matching difficulty, or {@code null} when no difficulty matches
     */
    @Nullable
    public static Difficulty fromString(String input) {
        input = input.toLowerCase();

        for (Difficulty d : values()) {
            if (d.aliases.contains(input)) {
                return d;
            }
        }

        return null;
    }

    @Override
    public @NonNull String translationKey() {
        return "options.difficulty." + this.name().toLowerCase(java.util.Locale.ENGLISH);
    }
}
