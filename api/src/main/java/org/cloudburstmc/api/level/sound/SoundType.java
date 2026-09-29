package org.cloudburstmc.api.level.sound;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;

/**
 * Identifies a named sound supplied by a resource pack. Its definition determines
 * the sound category, samples, and attenuation.
 */
public class SoundType implements net.kyori.adventure.sound.Sound.Type {
    private final Identifier id;
    private final Key key;

    private SoundType(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.getNamespace().isEmpty() || id.getName().isEmpty()) {
            throw new IllegalArgumentException("Sound identifier must not be empty");
        }
        this.key = Key.key(id.toString(), ':');
    }

    /**
     * Creates a sound reference without requiring a built-in definition.
     * The sound must exist in the listener's resource packs to be audible.
     *
     * @param id the sound identifier
     * @return the sound reference
     */
    public static SoundType of(Identifier id) {
        return new SoundType(id);
    }

    /**
     * Returns the sound identifier.
     *
     * @return the sound identifier
     */
    public Identifier getId() {
        return this.id;
    }

    @Override
    public @NonNull Key key() {
        return this.key;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SoundType sound && this.id.equals(sound.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
