package org.cloudburstmc.api.map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;

import java.awt.*;
import java.util.Objects;

/**
 * A map cursor. Coordinates range from -128 through 127 and rotation from 0 through 15.
 * Each coordinate unit is half a map pixel. Positive X points right and positive Y points down.
 *
 * @param x             the horizontal offset from the map center
 * @param y             the vertical offset from the map center
 * @param rotation      the clockwise sixteenth-turn rotation
 * @param type          the cursor appearance
 * @param caption       the cursor label, or an empty string
 * @param color         the cursor tint
 * @param trackedEntity the entity represented by this cursor, or null for a fixed marker
 */
public record MapCursor(int x, int y, int rotation, MapCursorType type, String caption, Color color, @Nullable Entity trackedEntity) {

    public MapCursor {
        if (x < -128 || x > 127 || y < -128 || y > 127 || rotation < 0 || rotation > 15) {
            throw new IllegalArgumentException("Invalid map cursor position or rotation");
        }

        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(caption, "caption");
        Objects.requireNonNull(color, "color");
    }
}
