package org.cloudburstmc.server.level;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface ChunkManager {

    default BlockState getBlockAt(Vector3i pos) {
        return getBlockAt(pos.getX(), pos.getY(), pos.getZ(), 0);
    }

    default BlockState getBlockAt(Vector3i pos, int layer) {
        return getBlockAt(pos.getX(), pos.getY(), pos.getZ(), layer);
    }

    default BlockState getBlockAt(int x, int y, int z) {
        return this.getBlockAt(x, y, z, 0);
    }

    BlockState getBlockAt(int x, int y, int z, int layer);

    default void setBlockAt(Vector3i pos, BlockState blockState) {
        this.setBlockAt(pos.getX(), pos.getY(), pos.getZ(), 0, blockState);
    }

    default void setBlockAt(Vector3i pos, int layer, BlockState blockState) {
        this.setBlockAt(pos.getX(), pos.getY(), pos.getZ(), layer, blockState);
    }

    default void setBlockAt(int x, int y, int z, BlockState blockState) {
        this.setBlockAt(x, y, z, 0, blockState);
    }

    void setBlockAt(int x, int y, int z, int layer, BlockState blockState);

    default Chunk getChunk(Vector3i pos) {
        return getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    default Chunk getChunk(Vector2i chunkPos) {
        return getChunk(chunkPos.getX(), chunkPos.getY());
    }

    Chunk getChunk(int chunkX, int chunkZ);

    long getSeed();
}
