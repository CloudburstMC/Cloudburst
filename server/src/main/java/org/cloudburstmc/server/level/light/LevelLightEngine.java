package org.cloudburstmc.server.level.light;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LevelLightEngine {

    public static void initialize(CloudLevel level, CloudChunk chunk) {
        List<Vector3i> blockUpdates;
        List<Vector3i> skyUpdates = List.of();
        try (LockedChunk locked = chunk.lockForWrite()) {
            blockUpdates = BlockLightEngine.initialize(new ChunkLightAccess(level, LightAccess.Type.BLOCK, locked), chunk.getX(), chunk.getZ());
            if (level.getDimension() == CloudLevel.DIMENSION_OVERWORLD) {
                skyUpdates = SkyLightEngine.initialize(new ChunkLightAccess(level, LightAccess.Type.SKY, locked), chunk.getX(), chunk.getZ());
            }
        }

        BlockLightEngine.update(level, blockUpdates);
        if (!skyUpdates.isEmpty()) {
            SkyLightEngine.update(level, skyUpdates);
        }
    }

    public static void updateBoundary(CloudLevel level, int chunkX, int chunkZ) {
        List<Vector3i> boundary = boundary(chunkX, chunkZ, level.getMinHeight(), level.getMaxHeight());
        BlockLightEngine.update(level, boundary);
        if (level.getDimension() == CloudLevel.DIMENSION_OVERWORLD) {
            SkyLightEngine.update(level, boundary);
        }
    }

    private static List<Vector3i> boundary(int chunkX, int chunkZ, int minHeight, int maxHeight) {
        List<Vector3i> positions = new ArrayList<>((maxHeight - minHeight) * 60);
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int y = minHeight; y < maxHeight; y++) {
            for (int offset = 0; offset < 16; offset++) {
                positions.add(Vector3i.from(minX + offset, y, minZ));
                positions.add(Vector3i.from(minX + offset, y, minZ + 15));
                if (offset > 0 && offset < 15) {
                    positions.add(Vector3i.from(minX, y, minZ + offset));
                    positions.add(Vector3i.from(minX + 15, y, minZ + offset));
                }
            }
        }
        return positions;
    }
}
