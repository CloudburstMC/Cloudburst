package org.cloudburstmc.api.item.data;

import org.cloudburstmc.api.level.sound.SoundType;

import java.util.Objects;

/**
 * A playable jukebox track, its length in ticks, and its comparator signal.
 */
public record JukeboxSong(SoundType sound, int durationTicks, int comparatorOutput) {

    public JukeboxSong {
        Objects.requireNonNull(sound, "sound");
        if (durationTicks <= 0 || comparatorOutput < 0 || comparatorOutput > 15) {
            throw new IllegalArgumentException("Invalid jukebox duration or comparator output");
        }
    }
}
