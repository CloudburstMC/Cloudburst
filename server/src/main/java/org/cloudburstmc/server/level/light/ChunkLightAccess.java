package org.cloudburstmc.server.level.light;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.function.LongFunction;

/**
 * {@link LightAccess} over locked chunks.
 *
 * <p>{@code chunks} returns the already-locked chunk for a chunk key, or null
 * when the chunk is absent; only the last chunk is remembered, since
 * propagation mostly walks within one chunk at a time. Sections are read
 * through the locked chunk on every access, so the block and sky views over
 * the same chunks stay consistent: new sections are zero-filled and each
 * channel writes only its own nibble.
 */
final class ChunkLightAccess implements LightAccess {

    private final LightChannel channel;
    private final int minHeight;
    private final int maxHeight;
    private final LightStateCache states;
    private final LongFunction<LockedChunk> chunks;

    private boolean hasCached;
    private long cachedKey;
    private @Nullable LockedChunk cachedChunk;

    ChunkLightAccess(LightChannel channel, int minHeight, int maxHeight, LightStateCache states,
            LongFunction<LockedChunk> chunks) {
        this.channel = channel;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.states = states;
        this.chunks = chunks;
    }

    @Override
    public int minHeight() {
        return this.minHeight;
    }

    @Override
    public int maxHeight() {
        return this.maxHeight;
    }

    @Override
    public boolean isLoaded(int x, int z) {
        return this.chunkFor(x >> 4, z >> 4) != null;
    }

    @Override
    public BlockState primary(int x, int y, int z) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return BlockStates.AIR;
        }
        LockedChunk chunk = this.chunkFor(x >> 4, z >> 4);
        if (chunk == null) {
            return BlockStates.AIR;
        }
        return chunk.getBlockState(x & 15, y, z & 15, BlockLayer.PRIMARY);
    }

    @Override
    public BlockState secondary(int x, int y, int z) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return BlockStates.AIR;
        }
        LockedChunk chunk = this.chunkFor(x >> 4, z >> 4);
        if (chunk == null) {
            return BlockStates.AIR;
        }
        return chunk.getBlockState(x & 15, y, z & 15, BlockLayer.SECONDARY);
    }

    @Override
    public int light(int x, int y, int z) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return 0;
        }
        LockedChunk chunk = this.chunkFor(x >> 4, z >> 4);
        if (chunk == null) {
            return 0;
        }
        CloudChunkSection section = chunk.getSection((y - this.minHeight) >> 4);
        if (section == null) {
            return 0;
        }
        return this.channel == LightChannel.BLOCK
                ? section.getBlockLight(x & 15, y & 15, z & 15)
                : section.getSkyLight(x & 15, y & 15, z & 15);
    }

    @Override
    public void setLight(int x, int y, int z, int light) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return;
        }
        LockedChunk chunk = this.chunkFor(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }
        int index = (y - this.minHeight) >> 4;
        CloudChunkSection section = chunk.getSection(index);
        if (section == null) {
            if (light == 0) {
                return;
            }
            section = chunk.getOrCreateLightSection(index);
        }
        if (this.channel == LightChannel.BLOCK) {
            section.setBlockLight(x & 15, y & 15, z & 15, light);
        } else {
            section.setSkyLight(x & 15, y & 15, z & 15, light);
        }
    }

    @Override
    public LightStateCache states() {
        return this.states;
    }

    /**
     * Returns the already-locked chunk, or null when it is absent.
     */
    private @Nullable LockedChunk chunkFor(int chunkX, int chunkZ) {
        long key = CloudChunk.key(chunkX, chunkZ);
        if (!this.hasCached || key != this.cachedKey) {
            this.cachedKey = key;
            this.hasCached = true;
            this.cachedChunk = this.chunks.apply(key);
        }
        return this.cachedChunk;
    }
}
