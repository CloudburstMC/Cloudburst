package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongCollection;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * Synchronous light updates for {@link CloudLevel}; thin overloads over
 * {@link LightRegion}, removed in Task 5.
 */
@UtilityClass
public class LevelLightEngine {

    public static void initialize(CloudLevel level, CloudChunk chunk) {
        region(level).initialize(chunk.getX(), chunk.getZ());
    }

    public static void updateBoundary(CloudLevel level, int chunkX, int chunkZ) {
        region(level).unloaded(chunkX, chunkZ);
    }

    public static void update(CloudLevel level, LongCollection changed) {
        region(level).update(changed);
    }

    private static LightRegion region(CloudLevel level) {
        return new LightRegion(level.getServer().getBlockRegistry(), level.getMinHeight(), level.getMaxHeight(),
                level.getDimension() == CloudLevel.DIMENSION_OVERWORLD,
                key -> level.getLoadedChunk(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key)));
    }
}
