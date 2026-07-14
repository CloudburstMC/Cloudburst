package org.cloudburstmc.api.util;

import java.util.List;

/**
 * Collision or outline geometry made from one or more axis-aligned boxes.
 */
public interface VoxelShape {

    /**
     * Returns whether this shape contains no boxes.
     *
     * @return {@code true} if this shape has no volume
     */
    boolean isEmpty();

    /**
     * Returns a single bounding box that contains the whole shape.
     *
     * @return the bounds of this shape
     * @throws UnsupportedOperationException if this shape is empty
     */
    BoundingBox bounds();

    /**
     * Returns the individual boxes that make up this shape.
     *
     * @return an immutable list of boxes
     */
    List<BoundingBox> getBoundingBoxes();

    /**
     * Moves this shape by the supplied offset.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return the moved shape
     */
    VoxelShape move(float x, float y, float z);

    /**
     * Tests whether this shape overlaps a bounding box.
     *
     * @param box the box to test
     * @return {@code true} if any box in this shape overlaps the supplied box
     */
    boolean overlaps(BoundingBox box);

    /**
     * Tests whether this shape overlaps a bounding box after applying an offset to this shape.
     *
     * @param box     the box to test
     * @param offsetX the x offset applied to this shape
     * @param offsetY the y offset applied to this shape
     * @param offsetZ the z offset applied to this shape
     * @return {@code true} if any offset box in this shape overlaps the supplied box
     */
    boolean overlaps(BoundingBox box, float offsetX, float offsetY, float offsetZ);

    /**
     * Resolves movement along one axis against this shape.
     *
     * @param axis     the movement axis
     * @param box      the moving box
     * @param movement the requested movement along the axis
     * @param offsetX  the x offset applied to this shape
     * @param offsetY  the y offset applied to this shape
     * @param offsetZ  the z offset applied to this shape
     * @return the movement that remains after collision resolution
     */
    float collide(Direction.Axis axis, BoundingBox box, float movement, float offsetX, float offsetY, float offsetZ);

    /**
     * Resolves movement along one axis against this shape.
     *
     * @param axis     the movement axis
     * @param box      the moving box
     * @param movement the requested movement along the axis
     * @return the movement that remains after collision resolution
     */
    default float collide(Direction.Axis axis, BoundingBox box, float movement) {
        return this.collide(axis, box, movement, 0, 0, 0);
    }

    /**
     * Visits each box in this shape.
     *
     * @param consumer the box consumer
     */
    void forEachBox(BoxConsumer consumer);

    /**
     * Visits each box in this shape after applying an offset.
     *
     * @param offsetX  the x offset applied to each box
     * @param offsetY  the y offset applied to each box
     * @param offsetZ  the z offset applied to each box
     * @param consumer the box consumer
     */
    void forEachBox(float offsetX, float offsetY, float offsetZ, BoxConsumer consumer);

    /**
     * Tests boxes in this shape until one matches.
     *
     * @param predicate the predicate to test each box
     * @return {@code true} if any box matches
     */
    boolean anyBox(BoxPredicate predicate);

    /**
     * Tests offset boxes in this shape until one matches.
     *
     * @param offsetX   the x offset applied to each box
     * @param offsetY   the y offset applied to each box
     * @param offsetZ   the z offset applied to each box
     * @param predicate the predicate to test each box
     * @return {@code true} if any box matches
     */
    boolean anyBox(float offsetX, float offsetY, float offsetZ, BoxPredicate predicate);

    /**
     * Consumes the coordinates of one box in a shape.
     */
    @FunctionalInterface
    interface BoxConsumer {

        /**
         * Accepts one box.
         *
         * @param minX the minimum x bound
         * @param minY the minimum y bound
         * @param minZ the minimum z bound
         * @param maxX the maximum x bound
         * @param maxY the maximum y bound
         * @param maxZ the maximum z bound
         */
        void accept(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
    }

    /**
     * Tests the coordinates of one box in a shape.
     */
    @FunctionalInterface
    interface BoxPredicate {

        /**
         * Tests one box.
         *
         * @param minX the minimum x bound
         * @param minY the minimum y bound
         * @param minZ the minimum z bound
         * @param maxX the maximum x bound
         * @param maxY the maximum y bound
         * @param maxZ the maximum z bound
         * @return {@code true} if the box matches
         */
        boolean test(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
    }
}
