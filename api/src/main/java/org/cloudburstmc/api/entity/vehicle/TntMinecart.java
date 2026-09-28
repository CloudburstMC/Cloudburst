package org.cloudburstmc.api.entity.vehicle;

public interface TntMinecart extends Minecart {

    /**
     * Returns the remaining fuse ticks, or {@code -1} while unprimed.
     */
    int getFuse();

    /**
     * Sets the fuse ticks, or {@code -1} to leave the minecart unprimed. Values below {@code -1} are invalid.
     */
    void setFuse(int fuse);
}
