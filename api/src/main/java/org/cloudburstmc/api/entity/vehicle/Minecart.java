package org.cloudburstmc.api.entity.vehicle;

/**
 * A rail vehicle that can display a block above its chassis.
 */
public interface Minecart extends Vehicle {

    /**
     * Returns the vertical display-block offset in sixteenths of a block.
     */
    int getDisplayOffset();

    /**
     * Sets the vertical display-block offset in sixteenths of a block.
     */
    void setDisplayOffset(int offset);
}
