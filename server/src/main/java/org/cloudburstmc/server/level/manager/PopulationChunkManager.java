package org.cloudburstmc.server.level.manager;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.LockableChunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.generator.GenerationRegion;

import java.util.Arrays;
import java.util.Objects;

/**
 * Locked generation region centered on the chunk being populated.
 */
public final class PopulationChunkManager implements GenerationRegion {
    private static final int DIAMETER_IN_CHUNKS = 3;
    private static final int BUFFER_IN_BLOCKS = 16;

    @Getter
    private final long seed;
    private final LockableChunk[] chunks = new LockableChunk[DIAMETER_IN_CHUNKS * DIAMETER_IN_CHUNKS];
    private final int centerChunkX;
    private final int centerChunkZ;
    private final int cornerChunkX;
    private final int cornerChunkZ;

    public PopulationChunkManager(Chunk center, LockableChunk[] chunks, long seed) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(chunks, "chunks");
        Preconditions.checkArgument(chunks.length == this.chunks.length,
                "Generation region requires %s chunks, received %s", this.chunks.length, chunks.length);

        this.seed = seed;
        this.centerChunkX = center.getX();
        this.centerChunkZ = center.getZ();
        this.cornerChunkX = this.centerChunkX - 1;
        this.cornerChunkZ = this.centerChunkZ - 1;

        for (LockableChunk chunk : chunks) {
            Objects.requireNonNull(chunk, "chunks contains null");
            this.chunks[this.chunkIndex(chunk.getX(), chunk.getZ())] = chunk;
        }

        Preconditions.checkArgument(Arrays.stream(this.chunks).allMatch(Objects::nonNull),
                "Generation region does not contain every chunk surrounding (%s,%s)",
                this.centerChunkX, this.centerChunkZ);
    }

    @Override
    public int getCenterChunkX() {
        return this.centerChunkX;
    }

    @Override
    public int getCenterChunkZ() {
        return this.centerChunkZ;
    }

    @Override
    public int getBuffer() {
        return BUFFER_IN_BLOCKS;
    }

    @Override
    public boolean containsBlock(int x, int z) {
        return this.containsChunk(x >> 4, z >> 4);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, 0);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z, int layer) {
        return this.chunkFromBlock(x, z).getBlockState(x & 0xF, y, z & 0xF, layer);
    }

    @Override
    public BlockState getBlockState(Vector3i position) {
        return this.getBlockState(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean setBlockState(int x, int y, int z, BlockState state) {
        return this.setBlockState(x, y, z, 0, state);
    }

    @Override
    public boolean setBlockState(int x, int y, int z, int layer, BlockState state) {
        this.chunkFromBlock(x, z).setBlockState(x & 0xF, y, z & 0xF, layer, state);
        return true;
    }

    @Override
    public boolean setBlockState(Vector3i position, BlockState state) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), state);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ) {
        return this.chunks[this.chunkIndex(chunkX, chunkZ)];
    }

    private LockableChunk chunkFromBlock(int blockX, int blockZ) {
        Preconditions.checkArgument(this.containsBlock(blockX, blockZ), "Block position (%s,%s) is outside the generation region", blockX, blockZ);
        return this.chunks[this.chunkIndex(blockX >> 4, blockZ >> 4)];
    }

    private int chunkIndex(int chunkX, int chunkZ) {
        int relativeX = chunkX - this.cornerChunkX;
        int relativeZ = chunkZ - this.cornerChunkZ;
        Preconditions.checkArgument(
                relativeX >= 0 && relativeX < DIAMETER_IN_CHUNKS
                        && relativeZ >= 0 && relativeZ < DIAMETER_IN_CHUNKS,
                "Chunk position (%s,%s) is outside the generation region", chunkX, chunkZ);
        return relativeX * DIAMETER_IN_CHUNKS + relativeZ;
    }

    private boolean containsChunk(int chunkX, int chunkZ) {
        int relativeX = chunkX - this.cornerChunkX;
        int relativeZ = chunkZ - this.cornerChunkZ;
        return relativeX >= 0 && relativeX < DIAMETER_IN_CHUNKS && relativeZ >= 0 && relativeZ < DIAMETER_IN_CHUNKS;
    }
}
