package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An entity launched by a projectile source.
 */
public interface Projectile extends Entity {

    /**
     * Returns the source that launched this projectile.
     *
     * @return the shooter, or {@code null}
     */
    @Nullable
    ProjectileSource getShooter();

    /**
     * Changes the source that launched this projectile.
     *
     * @param shooter the shooter, or {@code null}
     */
    void setShooter(@Nullable ProjectileSource shooter);
}
