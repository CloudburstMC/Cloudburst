package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Creature;

/**
 * A snow golem.
 */
public interface SnowGolem extends Creature {

    /**
     * Returns whether this snow golem has had its pumpkin removed.
     *
     * @return {@code true} if the pumpkin has been removed
     */
    boolean isDerp();

    /**
     * Sets whether this snow golem has had its pumpkin removed.
     *
     * @param derp whether the pumpkin is removed
     */
    void setDerp(boolean derp);
}
