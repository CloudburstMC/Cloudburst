package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.level.Level;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class LevelUnloadEvent extends LevelEvent implements Cancellable {

    public LevelUnloadEvent(Level level) {
        super(level);
    }

}
