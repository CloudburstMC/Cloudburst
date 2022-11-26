package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class SpawnChangeEvent extends LevelEvent {

    private final Vector3f previousSpawn;

    public SpawnChangeEvent(Level level, Vector3f previousSpawn) {
        super(level);
        this.previousSpawn = previousSpawn;
    }

    public Vector3f getPreviousSpawn() {
        return previousSpawn;
    }
}
