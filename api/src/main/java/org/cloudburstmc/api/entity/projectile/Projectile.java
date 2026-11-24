package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;

/**
 * A projectile that can be launched with or without a shooter.
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
