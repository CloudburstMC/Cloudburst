package org.cloudburstmc.server.command.argument;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Optional selector origin override.
 *
 * <p>A {@code null} component inherits the command source coordinate for that axis.</p>
 *
 * @param x absolute x coordinate, or {@code null} to use the command source x
 * @param y absolute y coordinate, or {@code null} to use the command source y
 * @param z absolute z coordinate, or {@code null} to use the command source z
 */
public record CloudSelectorPosition(@Nullable Float x, @Nullable Float y, @Nullable Float z) {

    /**
     * Resolves this position against a command source origin.
     *
     * @param origin fallback position for unset axes
     * @return absolute selector origin
     */
    public Vector3f resolve(Vector3f origin) {
        return Vector3f.from(
                this.x == null ? origin.getX() : this.x,
                this.y == null ? origin.getY() : this.y,
                this.z == null ? origin.getZ() : this.z
        );
    }
}
