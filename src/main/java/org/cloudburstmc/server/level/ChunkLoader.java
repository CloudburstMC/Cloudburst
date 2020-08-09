package org.cloudburstmc.server.level;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface ChunkLoader {

    int getLoaderId();

    boolean isLoaderActive();

    Vector3f getPosition();

    float getX();

    float getZ();

    Level getLevel();

    void onChunkChanged(Chunk chunk);

    void onChunkLoaded(Chunk chunk);

    void onChunkUnloaded(Chunk chunk);
}
