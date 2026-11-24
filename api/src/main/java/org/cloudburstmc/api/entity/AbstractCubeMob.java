package org.cloudburstmc.api.entity;

/**
 * A cube mob whose size determines its dimensions and base health.
 */
public interface AbstractCubeMob extends Mob {

    /**
     * Returns this mob's size.
     */
    int getSize();

    /**
     * Resizes this mob and restores its health if it is alive.
     * The size is clamped to the range supported by the species.
     */
    void setSize(int size);
}
