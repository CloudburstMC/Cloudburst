package org.cloudburstmc.server.level.collision;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.*;

/**
 * Factory and utility methods for voxel shapes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CloudVoxelShapes {

    private static final int FULL_BLOCK_CACHE_SIZE = 512;
    private static final long MAX_COVERAGE_CELLS = 262_144;

    /**
     * Tolerance used by shape collision and full-block checks.
     */
    public static final float EPSILON = 1.0E-7f;

    private static final VoxelShape EMPTY = new CloudVoxelShape(new float[0]);
    private static final VoxelShape BLOCK = box(0, 0, 0, 1, 1, 1);
    private static final Map<VoxelShape, Boolean> FULL_BLOCK_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(FULL_BLOCK_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<VoxelShape, Boolean> eldest) {
                    return size() > FULL_BLOCK_CACHE_SIZE;
                }
            }
    );

    /**
     * Returns the shared empty shape.
     *
     * @return a shape with no boxes
     */
    public static VoxelShape empty() {
        return EMPTY;
    }

    /**
     * Returns the shared full block shape.
     *
     * @return a shape covering the unit block from {@code 0, 0, 0} to {@code 1, 1, 1}
     */
    public static VoxelShape block() {
        return BLOCK;
    }

    /**
     * Creates a shape from a single box.
     *
     * @param minX the minimum x bound
     * @param minY the minimum y bound
     * @param minZ the minimum z bound
     * @param maxX the maximum x bound
     * @param maxY the maximum y bound
     * @param maxZ the maximum z bound
     * @return the shape, or the empty shape when any maximum bound equals its minimum
     * @throws IllegalArgumentException if any minimum bound is greater than its maximum
     */
    public static VoxelShape box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        validateBounds(minX, minY, minZ, maxX, maxY, maxZ);
        if (maxX == minX || maxY == minY || maxZ == minZ) {
            return empty();
        }

        return new CloudVoxelShape(new float[]{minX, minY, minZ, maxX, maxY, maxZ});
    }

    /**
     * Creates a shape from packed box coordinates.
     *
     * <p>Each box is represented by six values:
     * {@code minX, minY, minZ, maxX, maxY, maxZ}.</p>
     *
     * @param boxes packed box coordinates
     * @return the created shape, with zero-volume boxes ignored
     * @throws NullPointerException     if {@code boxes} is {@code null}
     * @throws IllegalArgumentException if the number of coordinates is not a multiple of six
     * @throws IllegalArgumentException if any minimum bound is greater than its maximum
     */
    public static VoxelShape fromBoxes(float... boxes) {
        Objects.requireNonNull(boxes, "boxes");
        if (boxes.length == 0) {
            return empty();
        }

        if (boxes.length % 6 != 0) {
            throw new IllegalArgumentException("Voxel shape box data must be a multiple of 6");
        }

        float[] validBoxes = new float[boxes.length];
        int validLength = 0;
        for (int i = 0; i < boxes.length; i += 6) {
            float minX = boxes[i];
            float minY = boxes[i + 1];
            float minZ = boxes[i + 2];
            float maxX = boxes[i + 3];
            float maxY = boxes[i + 4];
            float maxZ = boxes[i + 5];
            validateBounds(minX, minY, minZ, maxX, maxY, maxZ);
            if (maxX == minX || maxY == minY || maxZ == minZ) {
                continue;
            }

            validBoxes[validLength++] = minX;
            validBoxes[validLength++] = minY;
            validBoxes[validLength++] = minZ;
            validBoxes[validLength++] = maxX;
            validBoxes[validLength++] = maxY;
            validBoxes[validLength++] = maxZ;
        }

        if (validLength == 0) {
            return empty();
        }

        return new CloudVoxelShape(validLength == boxes.length ? boxes : Arrays.copyOf(validBoxes, validLength));
    }

    /**
     * Tests whether a shape overlaps a box after applying an offset to the shape.
     *
     * @param shape   the shape to test
     * @param box     the box to test
     * @param offsetX the x offset applied to the shape
     * @param offsetY the y offset applied to the shape
     * @param offsetZ the z offset applied to the shape
     * @return {@code true} if the offset shape overlaps the box
     */
    public static boolean overlaps(VoxelShape shape, BoundingBox box, float offsetX, float offsetY, float offsetZ) {
        return !shape.isEmpty() && shape.overlaps(box, offsetX, offsetY, offsetZ);
    }

    /**
     * Tests whether the union of {@code covering} fully contains the union of {@code required}.
     * Box boundaries from both shapes partition the tested volume into cells whose membership is constant.
     *
     * @param covering the shape providing coverage
     * @param required the shape that must be covered
     * @return {@code true} if {@code required} has no volume outside {@code covering}
     */
    public static boolean covers(VoxelShape covering, VoxelShape required) {
        Objects.requireNonNull(covering, "covering");
        Objects.requireNonNull(required, "required");
        if (covering == required || required.isEmpty()) {
            return true;
        }

        if (covering.isEmpty()) {
            return false;
        }

        BoundingBox bounds = required.bounds();
        BoundingBox coveringBounds = covering.bounds();
        if (coveringBounds.getMinX() > bounds.getMinX() + EPSILON
                || coveringBounds.getMinY() > bounds.getMinY() + EPSILON
                || coveringBounds.getMinZ() > bounds.getMinZ() + EPSILON
                || coveringBounds.getMaxX() < bounds.getMaxX() - EPSILON
                || coveringBounds.getMaxY() < bounds.getMaxY() - EPSILON
                || coveringBounds.getMaxZ() < bounds.getMaxZ() - EPSILON) {
            return false;
        }

        List<Float> xBoundaries = new ArrayList<>();
        List<Float> yBoundaries = new ArrayList<>();
        List<Float> zBoundaries = new ArrayList<>();

        addShapeBoundaries(required, bounds, xBoundaries, yBoundaries, zBoundaries);
        addShapeBoundaries(covering, bounds, xBoundaries, yBoundaries, zBoundaries);

        xBoundaries.sort(Float::compare);
        yBoundaries.sort(Float::compare);
        zBoundaries.sort(Float::compare);

        long cellCount = (long) (xBoundaries.size() - 1) * (yBoundaries.size() - 1) * (zBoundaries.size() - 1);
        if (cellCount > MAX_COVERAGE_CELLS) {
            throw new IllegalArgumentException("Voxel shape coverage would require " + cellCount
                            + " cells; maximum is " + MAX_COVERAGE_CELLS);
        }

        for (int x = 1; x < xBoundaries.size(); x++) {
            float midpointX = midpoint(xBoundaries, x);
            for (int y = 1; y < yBoundaries.size(); y++) {
                float midpointY = midpoint(yBoundaries, y);
                for (int z = 1; z < zBoundaries.size(); z++) {
                    float midpointZ = midpoint(zBoundaries, z);
                    if (contains(required, midpointX, midpointY, midpointZ) && !contains(covering, midpointX, midpointY, midpointZ)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Resolves a movement vector against a collection of collision shapes.
     *
     * @param box      the moving box
     * @param shapes   the shapes to collide against
     * @param movement the requested movement
     * @return the movement that remains after collision resolution
     */
    public static Vector3f collide(BoundingBox box, Iterable<VoxelShape> shapes, Vector3f movement) {
        float dx = movement.getX();
        float dy = movement.getY();
        float dz = movement.getZ();
        BoundingBox movedBox = box;

        for (Direction.Axis axis : Direction.axisStepOrder(dx, dz)) {
            switch (axis) {
                case X -> {
                    if (dx != 0) {
                        dx = collide(Direction.Axis.X, movedBox, shapes, dx);
                        movedBox = movedBox.move(dx, 0, 0);
                    }
                }
                case Y -> {
                    if (dy != 0) {
                        dy = collide(Direction.Axis.Y, movedBox, shapes, dy);
                        movedBox = movedBox.move(0, dy, 0);
                    }
                }
                case Z -> {
                    if (dz != 0) {
                        dz = collide(Direction.Axis.Z, movedBox, shapes, dz);
                        movedBox = movedBox.move(0, 0, dz);
                    }
                }
            }
        }

        return Vector3f.from(dx, dy, dz);
    }

    /**
     * Resolves movement along one axis against a collection of collision shapes.
     *
     * @param axis     the movement axis
     * @param box      the moving box
     * @param shapes   the shapes to collide against
     * @param movement the requested movement along the axis
     * @return the movement that remains after collision resolution
     */
    public static float collide(Direction.Axis axis, BoundingBox box, Iterable<VoxelShape> shapes, float movement) {
        float resolved = movement;
        for (VoxelShape shape : shapes) {
            if (Math.abs(resolved) < EPSILON) {
                return 0;
            }
            resolved = shape.collide(axis, box, resolved);
        }
        return resolved;
    }

    /**
     * Tests whether a shape covers a full unit block.
     *
     * @param shape the shape to test
     * @return {@code true} if the shape covers the unit block bounds
     */
    public static boolean isFullBlock(VoxelShape shape) {
        return FULL_BLOCK_CACHE.computeIfAbsent(shape, candidate -> candidate.covers(BLOCK));
    }

    private static void addShapeBoundaries(VoxelShape shape, BoundingBox bounds,
                                           List<Float> xBoundaries, List<Float> yBoundaries, List<Float> zBoundaries) {
        addBoundary(xBoundaries, bounds.getMinX());
        addBoundary(xBoundaries, bounds.getMaxX());
        addBoundary(yBoundaries, bounds.getMinY());
        addBoundary(yBoundaries, bounds.getMaxY());
        addBoundary(zBoundaries, bounds.getMinZ());
        addBoundary(zBoundaries, bounds.getMaxZ());
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            addBoundaryWithin(xBoundaries, minX, bounds.getMinX(), bounds.getMaxX());
            addBoundaryWithin(xBoundaries, maxX, bounds.getMinX(), bounds.getMaxX());
            addBoundaryWithin(yBoundaries, minY, bounds.getMinY(), bounds.getMaxY());
            addBoundaryWithin(yBoundaries, maxY, bounds.getMinY(), bounds.getMaxY());
            addBoundaryWithin(zBoundaries, minZ, bounds.getMinZ(), bounds.getMaxZ());
            addBoundaryWithin(zBoundaries, maxZ, bounds.getMinZ(), bounds.getMaxZ());
        });
    }

    private static void addBoundaryWithin(List<Float> boundaries, float value, float min, float max) {
        if (value > min && value < max) {
            addBoundary(boundaries, value);
        }
    }

    private static void addBoundary(List<Float> boundaries, float value) {
        if (!boundaries.contains(value)) {
            boundaries.add(value);
        }
    }

    private static float midpoint(List<Float> boundaries, int upperIndex) {
        return (boundaries.get(upperIndex - 1) + boundaries.get(upperIndex)) * 0.5f;
    }

    private static boolean contains(VoxelShape shape, float x, float y, float z) {
        return shape.anyBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                minX <= x && maxX >= x && minY <= y && maxY >= y && minZ <= z && maxZ >= z);
    }

    private static void validateBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) {
            throw new IllegalArgumentException("Shape bounds must be finite");
        }

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Shape minimum bounds must be less than or equal to maximum bounds");
        }
    }
}
