package org.cloudburstmc.server.event.level;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class SpawnChangeEvent extends WorldEvent {

    private final Vector3f previousSpawn;

    public SpawnChangeEvent(World world, Vector3f previousSpawn) {
        super(world);
        this.previousSpawn = previousSpawn;
    }

    public Vector3f getPreviousSpawn() {
        return previousSpawn;
    }
}
