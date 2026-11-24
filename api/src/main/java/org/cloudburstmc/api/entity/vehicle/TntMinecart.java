package org.cloudburstmc.api.entity.vehicle;

import org.cloudburstmc.api.entity.Explosive;

/**
 * A minecart carrying TNT that can be armed or detonated.
 */
public interface TntMinecart extends Minecart, Explosive {

    /**
     * Returns the remaining fuse ticks, or {@code -1} while unprimed.
     */
    int getFuseTicks();

    /**
     * Sets the fuse ticks, or {@code -1} to leave the minecart unprimed. Values below {@code -1} are invalid.
     */
    void setFuseTicks(int fuseTicks);

    /**
     * Attempts to detonate this minecart using its current speed.
     */
    void explode();
}
