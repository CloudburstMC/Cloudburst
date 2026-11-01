package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.chunk.LockedChunk;

import java.util.function.LongFunction;

public final class ChunkLightAccess implements LightAccess {

    private final LightChannel channel;
    private final int minHeight;
    private final int maxHeight;
    private final LightStateCache states;
    private final LongFunction<LockedChunk> chunks;

    private boolean hasCached;
    private long cachedKey;
    private LockedChunk cachedChunk;
    private CloudChunkSection[] cachedSections;

    public ChunkLightAccess(LightChannel channel, int minHeight, int maxHeight, LightStateCache states,
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
        LockedChunk chunk = this.chunkFor(x >> 4, z >> 4);
        if (chunk == null) {
            return BlockStates.AIR;
        }
        return chunk.getBlockState(x & 15, y, z & 15, BlockLayer.PRIMARY);
    }

    @Override
    public BlockState secondary(int x, int y, int z) {
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
        if (this.chunkFor(x >> 4, z >> 4) == null) {
            return 0;
        }
        CloudChunkSection section = this.cachedSections[(y - this.minHeight) >> 4];
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
        CloudChunkSection section = this.cachedSections[index];
        if (section == null) {
            if (light == 0) {
                return;
            }
            section = chunk.getOrCreateLightSection(index);
            this.cachedSections[index] = section;
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

    private LockedChunk chunkFor(int chunkX, int chunkZ) {
        long key = CloudChunk.key(chunkX, chunkZ);
        if (!this.hasCached || key != this.cachedKey) {
            this.cachedKey = key;
            this.hasCached = true;
            this.cachedChunk = this.chunks.apply(key);
            this.cachedSections = this.cachedChunk == null ? null : this.cachedChunk.getSections();
        }
        return this.cachedChunk;
    }
}
