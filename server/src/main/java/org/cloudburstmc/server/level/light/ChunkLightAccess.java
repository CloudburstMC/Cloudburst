package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.level.chunk.BlockLayerStorage;
import org.cloudburstmc.server.level.chunk.ChunkLightView;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.utils.NibbleArray;

import java.util.function.LongFunction;

/**
 * Light access restricted to borrowed chunk-section views.
 * Use on the lock-owning thread and discard before releasing those views.
 * Light writes do not mark chunks dirty.
 */
public class ChunkLightAccess implements LightAccess {

    private final LightChannel channel;
    private final int minHeight;
    private final int maxHeight;
    private final LightStateCache states;
    private final LongFunction<? extends ChunkLightView> chunks;

    private boolean hasCached;
    private long cachedKey;
    private @Nullable ChunkLightView cachedChunk;

    private boolean hasSectionCached;
    private long sectionCachedKey;
    private int sectionCachedIndex;
    private @Nullable CloudChunkSection sectionCached;

    private final Long2ObjectMap<CloudChunkSection[]> sectionsByKey = new Long2ObjectOpenHashMap<>();

    /**
     * @param channel   the light channel to read and write
     * @param minHeight the lowest accessible y
     * @param maxHeight one past the highest accessible y
     * @param states    the thread-confined block-state cache
     * @param chunks    lookup of borrowed views held under write locks, returning null when absent
     */
    public ChunkLightAccess(LightChannel channel, int minHeight, int maxHeight, LightStateCache states, LongFunction<? extends ChunkLightView> chunks) {
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
    public LightStateCache states() {
        return this.states;
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
            ChunkLightView chunk = this.chunkForKey(key);

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

    private @Nullable CloudChunkSection sectionFor(int x, int y, int z) {
        if (y < this.minHeight || y >= this.maxHeight) {
            return null;
        }

        long key = CloudChunk.key(x >> 4, z >> 4);
        int index = (y - this.minHeight) >> 4;

        if (this.hasSectionCached && key == this.sectionCachedKey && index == this.sectionCachedIndex) {
            return this.sectionCached;
        }

        ChunkLightView chunk = this.chunkForKey(key);

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

    private CloudChunkSection[] sectionsFor(long key, ChunkLightView chunk) {
        CloudChunkSection[] sections = this.sectionsByKey.get(key);

        if (sections == null) {
            sections = chunk.getLightSections();
            this.sectionsByKey.put(key, sections);
        }

        return sections;
    }

    private @Nullable ChunkLightView chunkFor(int chunkX, int chunkZ) {
        return chunkForKey(CloudChunk.key(chunkX, chunkZ));
    }

    private @Nullable ChunkLightView chunkForKey(long key) {
        if (!this.hasCached || key != this.cachedKey) {
            this.cachedKey = key;
            this.hasCached = true;
            this.cachedChunk = this.chunks.apply(key);
        }

        return this.cachedChunk;
    }
}
