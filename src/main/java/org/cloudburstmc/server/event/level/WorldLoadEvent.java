package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class WorldLoadEvent extends WorldEvent {

    public WorldLoadEvent(World world) {
        super(world);
    }

}
