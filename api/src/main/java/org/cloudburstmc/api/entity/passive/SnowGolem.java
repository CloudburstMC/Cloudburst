package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Mob;
import org.cloudburstmc.api.entity.Shearable;

/**
 * A snow construct whose pumpkin can be removed by shearing.
 */
public interface SnowGolem extends Mob, Shearable {

    /**
     * Returns whether this snow golem is wearing a pumpkin.
     */
    boolean hasPumpkin();

    /**
     * Sets whether this snow golem is wearing a pumpkin.
     */
    void setPumpkin(boolean hasPumpkin);
}
