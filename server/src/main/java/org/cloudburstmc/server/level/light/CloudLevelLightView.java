package org.cloudburstmc.server.level.light;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.Objects;
import java.util.function.LongFunction;

/**
 * Supplies fixed lighting settings and a live lookup of finished chunks.
 */
public class CloudLevelLightView implements LightLevelView {

    private final BlockRegistry registry;
    private final int minHeight;
    private final int maxHeight;
    private final boolean skyLight;
    private final LongFunction<CloudChunk> chunks;

    /**
     * @param registry  the block registry
     * @param minHeight the lowest lit height
     * @param maxHeight one past the highest lit height
     * @param skyLight  whether the level carries skylight
     * @param chunks    thread-safe lookup of finished chunks, returning null when absent
     */
    public CloudLevelLightView(BlockRegistry registry, int minHeight, int maxHeight, boolean skyLight, LongFunction<CloudChunk> chunks) {
        if (minHeight >= maxHeight) {
            throw new IllegalArgumentException("minHeight must be below maxHeight");
        }

        this.registry = Objects.requireNonNull(registry, "registry");
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.skyLight = skyLight;
        this.chunks = Objects.requireNonNull(chunks, "chunks");
    }

    @Override
    public BlockRegistry registry() {
        return this.registry;
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
    public boolean hasSkyLight() {
        return this.skyLight;
    }

    @Override
    public @Nullable CloudChunk getChunkForLighting(int chunkX, int chunkZ) {
        return this.chunks.apply(CloudChunk.key(chunkX, chunkZ));
    }
}
