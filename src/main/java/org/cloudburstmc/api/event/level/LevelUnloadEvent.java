package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Level;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class LevelUnloadEvent extends LevelEvent implements Cancellable {

    public LevelUnloadEvent(Level level) {
        super(level);
    }

}
