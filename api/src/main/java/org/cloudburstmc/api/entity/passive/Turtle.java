package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Ageable;
import org.cloudburstmc.api.level.Location;

/**
 * A turtle with a persistent home beach location.
 */
public interface Turtle extends Ageable {

    /**
     * Returns the turtle's home block location.
     *
     * @return the home location
     */
    Location getHome();

    /**
     * Sets the home block location in the turtle's level.
     *
     * @param home the home location
     * @throws IllegalArgumentException if the location is in another level
     */
    void setHome(Location home);
}
