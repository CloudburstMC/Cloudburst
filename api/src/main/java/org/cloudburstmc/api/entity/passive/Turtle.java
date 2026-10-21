package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.level.Location;

/**
 * A turtle with an age and a home beach location.
 */
public interface Turtle extends Creature, EntityAgeable {

    /**
     * Returns the age in ticks. Negative values count toward adulthood, zero is
     * an adult ready to breed, and positive values are the breeding cooldown.
     *
     * @return the age in ticks
     */
    int getAge();

    /**
     * Sets the age in ticks. A negative age makes the turtle a baby.
     *
     * @param age the age in ticks
     */
    void setAge(int age);

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
