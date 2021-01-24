package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class WorldUnloadEvent extends WorldEvent implements Cancellable {

    public WorldUnloadEvent(World world) {
        super(world);
    }

}
