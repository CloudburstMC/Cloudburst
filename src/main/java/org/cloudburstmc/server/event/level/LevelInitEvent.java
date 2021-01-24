package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Deprecated()
public class LevelInitEvent extends WorldEvent {

    public LevelInitEvent(World world) {
        super(world);
    }

}
