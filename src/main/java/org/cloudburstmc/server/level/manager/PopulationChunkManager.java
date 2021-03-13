package org.cloudburstmc.server.level.manager;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.LockableChunk;

/**
 * Implementation of {@link ChunkManager} used during chunk population.
 *
 * @author DaPorkchop_
 */
public final class PopulationChunkManager implements ChunkManager {
    @Getter
    private final long seed;

    private final LockableChunk[] chunks = new LockableChunk[3 * 3];

    private final int cornerX;
    private final int cornerZ;

    public PopulationChunkManager(@NonNull Chunk chunk, @NonNull LockableChunk[] allChunks, long seed) {
        this.seed = seed;
        this.cornerX = chunk.getX() - 1;
        this.cornerZ = chunk.getZ() - 1;

        for (LockableChunk lockableChunk : allChunks) {
            this.chunks[this.chunkIndex(lockableChunk.getX(), lockableChunk.getZ())] = lockableChunk;
        }
    }

    private int chunkIndex(int chunkX, int chunkZ) {
        int relativeX = chunkX - this.cornerX;
        int relativeZ = chunkZ - this.cornerZ;
        Preconditions.checkArgument(relativeX >= 0 && relativeX < 3 && relativeZ >= 0 && relativeZ < 3, "Chunk position (%s,%s) out of population bounds", chunkX, chunkZ);
        return relativeX * 3 + relativeZ;
    }

    private LockableChunk chunkFromBlock(int blockX, int blockZ) {
        int relativeX = (blockX >> 4) - this.cornerX;
        int relativeZ = (blockZ >> 4) - this.cornerZ;
        Preconditions.checkArgument(relativeX >= 0 && relativeX < 3 && relativeZ >= 0 && relativeZ < 3, "Block position (%s,%s) out of population bounds", blockX, blockZ);
        return this.chunks[relativeX * 3 + relativeZ];
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return this.chunkFromBlock(x, z).getBlock(x & 0xF, y, z & 0xF, 0);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z, int layer) {
        return this.chunkFromBlock(x, z).getBlock(x & 0xF, y, z & 0xF, layer);
    }

    @Override
    public boolean setBlockState(int x, int y, int z, BlockState state) {
        this.chunkFromBlock(x, z).setBlock(x & 0xF, y, z & 0xF, 0, state);
        return true;
    }

    @Override
    public boolean setBlockState(int x, int y, int z, int layer, BlockState state) {
        this.chunkFromBlock(x, z).setBlock(x & 0xF, y, z & 0xF, layer, state);
        return true;
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ) {
        return this.chunks[this.chunkIndex(chunkX, chunkZ)];
    }
}
