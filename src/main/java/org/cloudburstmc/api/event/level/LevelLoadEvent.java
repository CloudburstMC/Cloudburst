package org.cloudburstmc.api.event.level;

import org.cloudburstmc.server.level.Level;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class LevelLoadEvent extends LevelEvent {

    public LevelLoadEvent(Level level) {
        super(level);
    }

}
