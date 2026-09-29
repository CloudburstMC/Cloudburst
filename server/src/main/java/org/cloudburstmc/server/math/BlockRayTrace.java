package org.cloudburstmc.server.math;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

/**
 * Visits block cells crossed by a line segment, nearest first.
 */
public class BlockRayTrace implements Iterable<Vector3i> {

    private final Vector3f start;
    private final Vector3f end;

    private BlockRayTrace(Vector3f start, Vector3f end) {
        this.start = requireNonNull(start, "start");
        this.end = requireNonNull(end, "end");
        if (nonFinite(start) || nonFinite(end)) {
            throw new IllegalArgumentException("Ray endpoints must be finite");
        }
    }

    public static BlockRayTrace of(Vector3f start, Vector3f direction, double distance) {
        requireNonNull(direction, "direction");
        if (!Double.isFinite(distance) || distance < 0 || nonFinite(direction) || distance > 0 && direction.lengthSquared() == 0) {
            throw new IllegalArgumentException("Ray direction and distance must be finite and valid");
        }

        return new BlockRayTrace(start, distance == 0 ? start : start.add(direction.normalize().mul(distance)));
    }

    public static BlockRayTrace of(Vector3f start, Vector3f end) {
        return new BlockRayTrace(start, end);
    }

    @Override
    public @NonNull Iterator<Vector3i> iterator() {
        return new CellIterator();
    }

    private static boolean nonFinite(Vector3f vector) {
        return !Float.isFinite(vector.getX()) || !Float.isFinite(vector.getY()) || !Float.isFinite(vector.getZ());
    }

    private class CellIterator implements Iterator<Vector3i> {

        private final float dx = end.getX() - start.getX();
        private final float dy = end.getY() - start.getY();
        private final float dz = end.getZ() - start.getZ();

        private final int stepX = dx > 0 ? 1 : dx < 0 ? -1 : 0;
        private final int stepY = dy > 0 ? 1 : dy < 0 ? -1 : 0;
        private final int stepZ = dz > 0 ? 1 : dz < 0 ? -1 : 0;

        private final double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : 1d / Math.abs(dx);
        private final double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : 1d / Math.abs(dy);
        private final double deltaZ = dz == 0 ? Double.POSITIVE_INFINITY : 1d / Math.abs(dz);

        private int x = start.getFloorX();
        private int y = start.getFloorY();
        private int z = start.getFloorZ();

        private double maxX = firstBoundary(start.getX(), x, dx);
        private double maxY = firstBoundary(start.getY(), y, dy);
        private double maxZ = firstBoundary(start.getZ(), z, dz);

        private boolean first = true;

        @Override
        public boolean hasNext() {
            return this.first || Math.min(this.maxX, Math.min(this.maxY, this.maxZ)) <= 1d;
        }

        @Override
        public Vector3i next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            if (this.first) {
                this.first = false;
            } else if (this.maxX <= this.maxY && this.maxX <= this.maxZ) {
                this.x += this.stepX;
                this.maxX += this.deltaX;
            } else if (this.maxY <= this.maxZ) {
                this.y += this.stepY;
                this.maxY += this.deltaY;
            } else {
                this.z += this.stepZ;
                this.maxZ += this.deltaZ;
            }

            return Vector3i.from(this.x, this.y, this.z);
        }

        private static double firstBoundary(float start, int cell, float delta) {
            if (delta > 0) {
                return (cell + 1d - start) / delta;
            }

            if (delta < 0) {
                return (start - cell) / -delta;
            }

            return Double.POSITIVE_INFINITY;
        }
    }
}
