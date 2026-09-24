package org.cloudburstmc.api.level;

import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * The segment and geometry rules for a block or combined level ray trace.
 * Coordinates are level positions, not a direction and distance.
 *
 * @param from      the segment start
 * @param to        the segment end
 * @param blocks    the block geometry to test
 * @param fluids    the liquids to test
 * @param collision the entity context used for collision shapes
 */
public record RayTraceContext(Vector3f from, Vector3f to, BlockShapeMode blocks, FluidCollisionMode fluids, CollisionContext collision) {

    public RayTraceContext {
        requireNonNull(from, "from");
        requireNonNull(to, "to");
        requireNonNull(blocks, "blocks");
        requireNonNull(fluids, "fluids");
        requireNonNull(collision, "collision");
        if (nonFinite(from) || nonFinite(to)) {
            throw new IllegalArgumentException("Ray trace endpoints must be finite");
        }
    }

    private static boolean nonFinite(Vector3f vector) {
        return !Float.isFinite(vector.getX()) || !Float.isFinite(vector.getY()) || !Float.isFinite(vector.getZ());
    }
}
