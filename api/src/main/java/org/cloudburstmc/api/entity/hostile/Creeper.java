package org.cloudburstmc.api.entity.hostile;

/**
 * An explosive monster that can become charged.
 */
public interface Creeper extends Monster {

    /**
     * Returns whether this creeper is charged.
     */
    boolean isPowered();

    /**
     * Changes whether this creeper is charged.
     */
    void setPowered(boolean powered);
}
