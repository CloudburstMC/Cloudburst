package org.cloudburstmc.api.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Immutable axis-aligned bounding box.
 *
 * <p>All constructors normalize their coordinates so the minimum values are less than or equal to
 * the matching maximum values.</p>
 */
@Getter
@EqualsAndHashCode
public final class BoundingBox {

    private static final float EPSILON = 1.0E-7f;

    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;

    /**
     * Creates a bounding box from two opposite corners.
     *
     * @param pos1 the first corner
     * @param pos2 the second corner
     */
    public BoundingBox(Vector3i pos1, Vector3i pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    /**
     * Creates a bounding box from two opposite corners.
     *
     * @param pos1 the first corner
     * @param pos2 the second corner
     */
    public BoundingBox(Vector3f pos1, Vector3f pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    /**
     * Creates a bounding box from raw coordinate bounds.
     *
     * @param minX one x bound
     * @param minY one y bound
     * @param minZ one z bound
     * @param maxX the other x bound
     * @param maxY the other y bound
     * @param maxZ the other z bound
     */
    public BoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    /**
     * Creates a one-block bounding box at the supplied block position.
     *
     * @param position the block position
     * @return a bounding box covering {@code position} to {@code position + 1}
     */
    public static BoundingBox unit(Vector3i position) {
        return new BoundingBox(position.toFloat(), position.toFloat().add(1, 1, 1));
    }

    /**
     * Returns a copy with a different minimum x bound.
     *
     * @param minX the new minimum x bound
     * @return the updated bounding box
     */
    public BoundingBox setMinX(float minX) {
        return new BoundingBox(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    /**
     * Returns a copy with a different minimum y bound.
     *
     * @param minY the new minimum y bound
     * @return the updated bounding box
     */
    public BoundingBox setMinY(float minY) {
        return new BoundingBox(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    /**
     * Returns a copy with a different minimum z bound.
     *
     * @param minZ the new minimum z bound
     * @return the updated bounding box
     */
    public BoundingBox setMinZ(float minZ) {
        return new BoundingBox(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
    }

    /**
     * Returns a copy with a different maximum x bound.
     *
     * @param maxX the new maximum x bound
     * @return the updated bounding box
     */
    public BoundingBox setMaxX(float maxX) {
        return new BoundingBox(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
    }

    /**
     * Returns a copy with a different maximum y bound.
     *
     * @param maxY the new maximum y bound
     * @return the updated bounding box
     */
    public BoundingBox setMaxY(float maxY) {
        return new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
    }

    /**
     * Returns a copy with a different maximum z bound.
     *
     * @param maxZ the new maximum z bound
     * @return the updated bounding box
     */
    public BoundingBox setMaxZ(float maxZ) {
        return new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
    }

    /**
     * Expands this box in the direction and magnitude of the supplied vector.
     *
     * <p>Negative components expand the minimum side of the box. Positive components expand the
     * maximum side.</p>
     *
     * @param movement the directional expansion vector
     * @return the expanded bounding box
     */
    public BoundingBox expandTowards(Vector3f movement) {
        return this.expandTowards(movement.getX(), movement.getY(), movement.getZ());
    }

    /**
     * Expands this box in the direction and magnitude of the supplied components.
     *
     * <p>Negative components expand the minimum side of the box. Positive components expand the
     * maximum side.</p>
     *
     * @param x the x expansion
     * @param y the y expansion
     * @param z the z expansion
     * @return the expanded bounding box
     */
    public BoundingBox expandTowards(float x, float y, float z) {
        float minX = this.minX;
        float minY = this.minY;
        float minZ = this.minZ;
        float maxX = this.maxX;
        float maxY = this.maxY;
        float maxZ = this.maxZ;

        if (x < 0) {
            minX += x;
        } else if (x > 0) {
            maxX += x;
        }

        if (y < 0) {
            minY += y;
        } else if (y > 0) {
            maxY += y;
        }

        if (z < 0) {
            minZ += z;
        } else if (z > 0) {
            maxZ += z;
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Expands this box equally in both directions on each axis.
     *
     * <p>Negative values shrink the box.</p>
     *
     * @param x the amount to add to both x sides
     * @param y the amount to add to both y sides
     * @param z the amount to add to both z sides
     * @return the inflated bounding box
     */
    public BoundingBox inflate(float x, float y, float z) {
        return new BoundingBox(this.minX - x, this.minY - y, this.minZ - z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    /**
     * Shrinks this box equally from both directions on each axis.
     *
     * @param x the amount to remove from both x sides
     * @param y the amount to remove from both y sides
     * @param z the amount to remove from both z sides
     * @return the deflated bounding box
     */
    public BoundingBox deflate(float x, float y, float z) {
        return this.inflate(-x, -y, -z);
    }

    /**
     * Moves this box by the supplied offset.
     *
     * @param movement the movement offset
     * @return the moved bounding box
     */
    public BoundingBox move(Vector3i movement) {
        return this.move(movement.getX(), movement.getY(), movement.getZ());
    }

    /**
     * Moves this box by the supplied offset.
     *
     * @param movement the movement offset
     * @return the moved bounding box
     */
    public BoundingBox move(Vector3f movement) {
        return this.move(movement.getX(), movement.getY(), movement.getZ());
    }

    /**
     * Moves this box by the supplied offset.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return the moved bounding box
     */
    public BoundingBox move(float x, float y, float z) {
        return new BoundingBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    /**
     * Tests whether this box overlaps another box.
     *
     * <p>Boxes that only touch at an edge or face are not considered intersecting.</p>
     *
     * @param box the box to test
     * @return {@code true} if the boxes overlap
     */
    public boolean intersects(BoundingBox box) {
        return this.intersects(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * Tests whether this box overlaps the supplied bounds.
     *
     * <p>Bounds that only touch this box at an edge or face are not considered intersecting.</p>
     *
     * @param minX the minimum x bound
     * @param minY the minimum y bound
     * @param minZ the minimum z bound
     * @param maxX the maximum x bound
     * @param maxY the maximum y bound
     * @param maxZ the maximum z bound
     * @return {@code true} if the bounds overlap this box
     */
    public boolean intersects(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.minX < maxX
                && this.maxX > minX
                && this.minY < maxY
                && this.maxY > minY
                && this.minZ < maxZ
                && this.maxZ > minZ;
    }

    /**
     * Tests whether this box contains the supplied position.
     *
     * @param vector the position to test
     * @return {@code true} if the position is inside this box
     */
    public boolean contains(Vector3f vector) {
        return this.contains(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Tests whether this box contains the supplied position.
     *
     * <p>Minimum bounds are inclusive. Maximum bounds are exclusive.</p>
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     * @return {@code true} if the position is inside this box
     */
    public boolean contains(float x, float y, float z) {
        return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
    }

    /**
     * Finds the first intersection between this box and a line segment.
     *
     * @param pos1 the segment start
     * @param pos2 the segment end
     * @return the hit result, or {@code null} when the segment misses this box
     */
    public @Nullable MovingObjectPosition clip(Vector3f pos1, Vector3f pos2) {
        float dx = pos2.getX() - pos1.getX();
        float dy = pos2.getY() - pos1.getY();
        float dz = pos2.getZ() - pos1.getZ();
        ClipResult result = null;

        if (dx > EPSILON) {
            result = clipPoint(result, dx, dy, dz, this.minX, this.minY, this.maxY, this.minZ, this.maxZ, 4,
                    pos1.getX(), pos1.getY(), pos1.getZ());
        } else if (dx < -EPSILON) {
            result = clipPoint(result, dx, dy, dz, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ, 5,
                    pos1.getX(), pos1.getY(), pos1.getZ());
        }

        if (dy > EPSILON) {
            result = clipPoint(result, dy, dz, dx, this.minY, this.minZ, this.maxZ, this.minX, this.maxX, 0,
                    pos1.getY(), pos1.getZ(), pos1.getX());
        } else if (dy < -EPSILON) {
            result = clipPoint(result, dy, dz, dx, this.maxY, this.minZ, this.maxZ, this.minX, this.maxX, 1,
                    pos1.getY(), pos1.getZ(), pos1.getX());
        }

        if (dz > EPSILON) {
            result = clipPoint(result, dz, dx, dy, this.minZ, this.minX, this.maxX, this.minY, this.maxY, 2,
                    pos1.getZ(), pos1.getX(), pos1.getY());
        } else if (dz < -EPSILON) {
            result = clipPoint(result, dz, dx, dy, this.maxZ, this.minX, this.maxX, this.minY, this.maxY, 3,
                    pos1.getZ(), pos1.getX(), pos1.getY());
        }

        if (result == null) {
            return null;
        }

        Vector3f hit = Vector3f.from(pos1.getX() + result.scale() * dx, pos1.getY() + result.scale() * dy, pos1.getZ() + result.scale() * dz);
        return MovingObjectPosition.fromBlock(Vector3i.ZERO, result.face(), hit);
    }

    private static @Nullable ClipResult clipPoint(@Nullable ClipResult current, float da, float db, float dc, float point,
                                                  float minB, float maxB, float minC, float maxC, int face,
                                                  float fromA, float fromB, float fromC) {
        float scale = (point - fromA) / da;
        float pointB = fromB + scale * db;
        float pointC = fromC + scale * dc;
        float closestScale = current == null ? 1 : current.scale();
        if (scale > 0 && scale < closestScale
                && pointB > minB - EPSILON && pointB < maxB + EPSILON
                && pointC > minC - EPSILON && pointC < maxC + EPSILON) {
            return new ClipResult(scale, face);
        }
        return current;
    }

    private record ClipResult(float scale, int face) {
    }

    @Override
    public String toString() {
        return "BoundingBox(" + this.minX + ", " + this.minY + ", " + this.minZ + ", " + this.maxX + ", " + this.maxY + ", " + this.maxZ + ")";
    }
}
