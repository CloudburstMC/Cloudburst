package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An entity launched by a projectile source.
 */
public interface Projectile extends Entity {

    /**
     * Returns the entity that launched this projectile.
     *
     * @return the shooter, or {@code null}
     */
    @Nullable
    default Entity getShooter() {
        return getOwner();
    }

    /**
     * Changes the entity that launched this projectile.
     *
     * @param entity the shooter, or {@code null}
     */
    default void setShooter(@Nullable Entity entity) {
        setOwner(entity);
    }
}
