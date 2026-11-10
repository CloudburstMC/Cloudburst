package org.cloudburstmc.api.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Context passed to block collision shape handlers.
 * <p>
 * A shape can differ between placement checks and entity movement.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CollisionContext {

    private static final float ABOVE_EPSILON = 1.0E-5f;
    private static final CollisionContext EMPTY = new CollisionContext(null, false, Float.NEGATIVE_INFINITY, false);
    private static final CollisionContext EMPTY_PLACEMENT = new CollisionContext(null, false, Float.NEGATIVE_INFINITY, true);

    private final @Nullable Entity entity;
    private final boolean descending;
    private final float entityBottom;
    private final boolean placement;

    /**
     * Returns a context with no entity.
     *
     * @return the empty collision context
     */
    public static CollisionContext empty() {
        return EMPTY;
    }

    /**
     * Creates a context for an entity.
     *
     * @param entity the entity, or {@code null} for the empty context
     * @return the collision context
     */
    public static CollisionContext of(@Nullable Entity entity) {
        if (entity == null) {
            return empty();
        }
        return new CollisionContext(entity, entity.getMotion().getY() < 0, entity.getBoundingBox().getMinY(), false);
    }

    /**
     * Creates a context with an explicit movement direction.
     *
     * @param entity the entity, or {@code null} for the empty context
     * @param descending whether the collision query is moving downward
     * @return the collision context
     */
    public static CollisionContext of(@Nullable Entity entity, boolean descending) {
        if (entity == null) {
            return empty();
        }
        return new CollisionContext(entity, descending, entity.getBoundingBox().getMinY(), false);
    }

    /**
     * Creates a context for checking whether a block can be placed.
     *
     * @param entity the entity placing the block, if any
     * @return the placement collision context
     */
    public static CollisionContext forPlacement(@Nullable Entity entity) {
        if (entity == null) {
            return EMPTY_PLACEMENT;
        }
        return new CollisionContext(entity, entity.getMotion().getY() < 0, entity.getBoundingBox().getMinY(), true);
    }

    /**
     * @return whether this query checks block placement rather than movement
     */
    public boolean isPlacement() {
        return this.placement;
    }

    /**
     * Returns whether this context contains an entity.
     *
     * @return {@code true} if an entity is present
     */
    public boolean hasEntity() {
        return this.entity != null;
    }

    /**
     * Tests whether the entity is above a shape at a block position.
     *
     * @param shape        the block-local shape
     * @param position     the block position
     * @param defaultValue the value to return when this context has no entity or the shape is empty
     * @return {@code true} if the entity bottom is above the top of the shape
     */
    public boolean isAbove(VoxelShape shape, Vector3i position, boolean defaultValue) {
        if (this.entity == null || shape.isEmpty()) {
            return defaultValue;
        }
        return this.entityBottom > position.getY() + shape.bounds().getMaxY() - ABOVE_EPSILON;
    }
}
