package org.cloudburstmc.api.level.sound;

import java.util.Objects;

/**
 * Describes named sound playback. Category and sample selection come from the
 * resource-pack definition rather than a category or random-seed override.
 *
 * @param sound          the sound to play
 * @param volume         the finite, non-negative volume multiplier
 * @param pitch          the finite, positive pitch multiplier
 * @param repeatCount    the non-negative number of repeats after the initial playback
 * @param ignoreDistance whether to bypass the listener distance check
 * @param startOffset    the finite, non-negative playback offset in seconds
 */
public record SoundPlayback(SoundType sound, float volume, float pitch, int repeatCount, boolean ignoreDistance, float startOffset) {

    public SoundPlayback {
        Objects.requireNonNull(sound, "sound");
        if (!Float.isFinite(volume) || volume < 0) {
            throw new IllegalArgumentException("Sound volume must be finite and non-negative");
        }

        if (!Float.isFinite(pitch) || pitch <= 0) {
            throw new IllegalArgumentException("Sound pitch must be finite and positive");
        }

        if (repeatCount < 0) {
            throw new IllegalArgumentException("Sound repeat count must not be negative");
        }

        if (!Float.isFinite(startOffset) || startOffset < 0) {
            throw new IllegalArgumentException("Sound playback offset must be finite and non-negative");
        }
    }

    /**
     * Creates one playback from the beginning, subject to normal distance checks.
     *
     * @param sound  the sound to play
     * @param volume the volume multiplier
     * @param pitch  the pitch multiplier
     */
    public SoundPlayback(SoundType sound, float volume, float pitch) {
        this(sound, volume, pitch, 0, false, 0);
    }
}
