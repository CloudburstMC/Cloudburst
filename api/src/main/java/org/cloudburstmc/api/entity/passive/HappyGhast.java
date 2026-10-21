package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityAgeable;

/**
 * A happy ghast, including its juvenile ghastling form.
 */
public interface HappyGhast extends Creature, EntityAgeable {

    /**
     * Returns the age in ticks. Negative values count toward adulthood.
     *
     * @return the age in ticks
     */
    int getAge();

    /**
     * Sets the age in ticks. A negative age makes the ghast a ghastling.
     *
     * @param age the age in ticks
     */
    void setAge(int age);
}
