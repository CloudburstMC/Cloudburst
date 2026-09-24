package org.cloudburstmc.api.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * A segment intersection with a bounding box, independent of level objects.
 */
public record BoxIntersection(Vector3f position, @Nullable Direction face) {

    public BoxIntersection {
        requireNonNull(position, "position");
    }
}
