package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class WorldEvent extends Event {

    private final World world;

    public WorldEvent(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }
}
