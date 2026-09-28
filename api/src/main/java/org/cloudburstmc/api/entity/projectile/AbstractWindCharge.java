package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;

/**
 * A wind charge that releases a burst of knockback on impact.
 */
public interface AbstractWindCharge extends Projectile {

    /**
     * Releases the charge's burst at its current position and removes the projectile.
     */
    void explode();
}
