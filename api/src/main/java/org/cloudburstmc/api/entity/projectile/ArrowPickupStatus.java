package org.cloudburstmc.api.entity.projectile;

/**
 * Determines which players may pick up a landed arrow-like projectile.
 */
public enum ArrowPickupStatus {
    /**
     * No player may pick up the projectile.
     */
    DISALLOWED,
    /**
     * Any eligible player may pick up the projectile.
     */
    ALLOWED,
    /**
     * Only creative-mode players may pick up the projectile.
     */
    CREATIVE_ONLY
}
