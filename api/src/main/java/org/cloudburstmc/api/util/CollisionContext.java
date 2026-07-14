package org.cloudburstmc.api.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Context passed to block collision shape handlers.
 *
 * <p>The context lets blocks adjust their collision shape for an entity. For example, a block can
 * expose a different shape when an entity is above it or moving downward.</p>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollisionContext {
    private static final float ABOVE_EPSILON = 1.0E-5f;
    private static final CollisionContext EMPTY = new CollisionContext(null, false, Float.NEGATIVE_INFINITY);

    private final @Nullable Entity entity;
    private final boolean descending;
    private final float entityBottom;

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
        return new CollisionContext(entity, entity.getMotion().getY() < 0, entity.getBoundingBox().getMinY());
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
