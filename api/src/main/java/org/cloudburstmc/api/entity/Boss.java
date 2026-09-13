package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.boss.BossBar;

/**
 * An entity represented by a boss bar.
 */
public interface Boss extends Entity {

    /**
     * Returns this entity's boss bar.
     *
     * @return the boss bar
     */
    BossBar getBossBar();
}
