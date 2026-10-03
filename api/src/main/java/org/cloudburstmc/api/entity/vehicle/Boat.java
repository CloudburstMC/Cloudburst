package org.cloudburstmc.api.entity.vehicle;

/**
 * A floating vehicle with ordered passenger seats.
 */
public interface Boat extends Vehicle {

    /**
     * @return the boat's material and hull style
     */
    BoatType getBoatType();

    /**
     * Changes the material and hull style without changing the boat's inventory or passengers.
     *
     * @param type the material and hull style to use
     */
    void setBoatType(BoatType type);

    /**
     * @return the most recently determined medium surrounding or supporting the boat
     */
    BoatStatus getStatus();

    /**
     * @return the maximum number of passengers
     */
    int getMaxPassengers();

    /**
     * @return whether the left paddle is moving
     */
    boolean isPaddlingLeft();

    /**
     * @return whether the right paddle is moving
     */
    boolean isPaddlingRight();
}
