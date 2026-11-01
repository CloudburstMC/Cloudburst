package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongCollection;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.function.LongFunction;

/**
 * Synchronous light updates for {@link CloudLevel}; thin synchronous overloads
 * over {@link LightRegion}.
 */
@UtilityClass
public class LevelLightEngine {

    public static void initialize(CloudLevel level, CloudChunk chunk) {
        long centerKey = CloudChunk.key(chunk.getX(), chunk.getZ());
        region(level, key -> key == centerKey ? chunk : level.getLoadedChunk(key))
                .initialize(chunk.getX(), chunk.getZ());
    }

    public static void updateBoundary(CloudLevel level, int chunkX, int chunkZ) {
        region(level).unloaded(chunkX, chunkZ);
    }

    public static void update(CloudLevel level, LongCollection changed) {
        region(level).update(changed);
    }

    private static LightRegion region(CloudLevel level) {
        return region(level, level::getLoadedChunk);
    }

    private static LightRegion region(CloudLevel level, LongFunction<CloudChunk> loadedChunks) {
        return new LightRegion(level.getServer().getBlockRegistry(), level.getMinHeight(), level.getMaxHeight(),
                level.getDimension() == CloudLevel.DIMENSION_OVERWORLD, loadedChunks);
    }
}
