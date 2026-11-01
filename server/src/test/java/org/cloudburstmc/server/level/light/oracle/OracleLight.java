package org.cloudburstmc.server.level.light.oracle;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.Collection;
import java.util.List;
import java.util.function.LongFunction;

@UtilityClass
public class OracleLight {

    public static void initialize(BlockRegistry registry, int minHeight, int maxHeight, LongFunction<Chunk> loadedChunks, CloudChunk chunk) {
        OracleChunkLightAccess blockAccess = new OracleChunkLightAccess(registry, minHeight, maxHeight, OracleLightAccess.Type.BLOCK, loadedChunks);
        List<Vector3i> blockUpdates = OracleBlockLightEngine.initialize(blockAccess, chunk.getX(), chunk.getZ());
        OracleChunkLightAccess skyAccess = new OracleChunkLightAccess(registry, minHeight, maxHeight, OracleLightAccess.Type.SKY, loadedChunks);
        List<Vector3i> skyUpdates = OracleSkyLightEngine.initialize(skyAccess, chunk.getX(), chunk.getZ());

        OracleBlockLightEngine.update(blockAccess, blockUpdates);
        OracleSkyLightEngine.update(skyAccess, skyUpdates);
    }

    public static void update(BlockRegistry registry, int minHeight, int maxHeight, LongFunction<Chunk> loadedChunks, Collection<Vector3i> changed) {
        OracleBlockLightEngine.update(new OracleChunkLightAccess(registry, minHeight, maxHeight, OracleLightAccess.Type.BLOCK, loadedChunks), changed);
        OracleSkyLightEngine.update(new OracleChunkLightAccess(registry, minHeight, maxHeight, OracleLightAccess.Type.SKY, loadedChunks), changed);
    }
}
