package org.cloudburstmc.server.event.level;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.level.Level;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class SpawnChangeEvent extends LevelEvent {

    private final Vector3f previousSpawn;

    public SpawnChangeEvent(Level level, Vector3f previousSpawn) {
        super(level);
        this.previousSpawn = previousSpawn;
    }

    public Vector3f getPreviousSpawn() {
        return previousSpawn;
    }
}
