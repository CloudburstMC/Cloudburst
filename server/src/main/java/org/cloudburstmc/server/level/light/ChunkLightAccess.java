package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.chunk.BlockLayerStorage;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.utils.NibbleArray;

import java.util.function.LongFunction;

/**
 * {@link LightAccess} over locked chunks.
 *
 * <p>{@code chunks} returns the already-locked chunk for a chunk key, or null
 * when the chunk is absent; only the last chunk is remembered, since
 * propagation mostly walks within one chunk at a time.
 *
 * <p>Each locked chunk's section array is snapshotted on first use, so block
 * states come straight from palette storage and light values straight from
 * the nibble arrays, without level lookups or per-cell bounds checks. A null
 * snapshot entry reads as air and 0, matching a zero-filled fresh section;
 * sections created mid-pass are written back into the snapshot, so later
 * reads in the same view see them. The block and sky views keep separate
 * snapshots and still agree, because a fresh section reads as air and 0 while
 * each channel writes only its own nibble.
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

    private boolean hasSectionCached;
    private long sectionCachedKey;
    private int sectionCachedIndex;
    private @Nullable CloudChunkSection sectionCached;

    private final Long2ObjectMap<CloudChunkSection[]> sectionsByKey = new Long2ObjectOpenHashMap<>();

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
        CloudChunkSection section = this.sectionFor(x, y, z);
        if (section == null) {
            return BlockStates.AIR;
        }
        return section.getBlockStorageArray()[BlockLayerStorage.index(BlockLayer.PRIMARY)]
                .getBlock(CloudChunkSection.blockIndex(x & 15, (y - this.minHeight) & 15, z & 15));
    }

    @Override
    public BlockState secondary(int x, int y, int z) {
        CloudChunkSection section = this.sectionFor(x, y, z);
        if (section == null) {
            return BlockStates.AIR;
        }
        return section.getBlockStorageArray()[BlockLayerStorage.index(BlockLayer.SECONDARY)]
                .getBlock(CloudChunkSection.blockIndex(x & 15, (y - this.minHeight) & 15, z & 15));
    }

    @Override
    public int light(int x, int y, int z) {
        CloudChunkSection section = this.sectionFor(x, y, z);
        if (section == null) {
            return 0;
        }
        NibbleArray nibbles = this.channel == LightChannel.BLOCK
                ? section.getBlockLightArray()
                : section.getSkyLightArray();
        return nibbles.get(CloudChunkSection.blockIndex(x & 15, (y - this.minHeight) & 15, z & 15));
    }

    @Override
    public void setLight(int x, int y, int z, int light) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return;
        }
        long key = CloudChunk.key(x >> 4, z >> 4);
        int index = (y - this.minHeight) >> 4;
        CloudChunkSection section = this.sectionFor(x, y, z);
        if (section == null) {
            LockedChunk chunk = this.chunkForKey(key);
            if (chunk == null) {
                return;
            }
            if (light == 0) {
                return;
            }
            section = chunk.getOrCreateLightSection(index);
            this.sectionsFor(key, chunk)[index] = section;
            this.sectionCachedKey = key;
            this.sectionCachedIndex = index;
            this.sectionCached = section;
            this.hasSectionCached = true;
        }
        NibbleArray nibbles = this.channel == LightChannel.BLOCK
                ? section.getBlockLightArray()
                : section.getSkyLightArray();
        nibbles.set(CloudChunkSection.blockIndex(x & 15, (y - this.minHeight) & 15, z & 15), (byte) light);
    }

    @Override
    public LightStateCache states() {
        return this.states;
    }

    /**
     * Returns the section holding the cell, or null for out-of-height cells
     * and cells in absent or ungenerated sections. The last section is
     * remembered, since most visits stay within one section at a time.
     */
    private @Nullable CloudChunkSection sectionFor(int x, int y, int z) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return null;
        }
        long key = CloudChunk.key(x >> 4, z >> 4);
        int index = (y - this.minHeight) >> 4;
        if (this.hasSectionCached && key == this.sectionCachedKey && index == this.sectionCachedIndex) {
            return this.sectionCached;
        }
        LockedChunk chunk = this.chunkForKey(key);
        if (chunk == null) {
            return null;
        }
        CloudChunkSection section = this.sectionsFor(key, chunk)[index];
        this.sectionCachedKey = key;
        this.sectionCachedIndex = index;
        this.sectionCached = section;
        this.hasSectionCached = true;
        return section;
    }

    /**
     * Returns the snapshotted sections of an already-locked chunk.
     */
    private CloudChunkSection[] sectionsFor(long key, LockedChunk chunk) {
        CloudChunkSection[] sections = this.sectionsByKey.get(key);
        if (sections == null) {
            sections = chunk.getSections();
            this.sectionsByKey.put(key, sections);
        }
        return sections;
    }

    /**
     * Returns the already-locked chunk, or null when it is absent.
     */
    private @Nullable LockedChunk chunkFor(int chunkX, int chunkZ) {
        return chunkForKey(CloudChunk.key(chunkX, chunkZ));
    }

    private @Nullable LockedChunk chunkForKey(long key) {
        if (!this.hasCached || key != this.cachedKey) {
            this.cachedKey = key;
            this.hasCached = true;
            this.cachedChunk = this.chunks.apply(key);
        }
        return this.cachedChunk;
    }
}
