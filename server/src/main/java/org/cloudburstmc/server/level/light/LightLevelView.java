package org.cloudburstmc.server.level.light;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * The level state the light scheduler reads.
 *
 * <p>{@link #registry()}, {@link #minHeight()}, {@link #maxHeight()} and
 * {@link #hasSkyLight()} are stable for the life of the level.
 * {@link #loadedChunk(int, int)} is called from the background lane and must
 * be thread-safe.
 */
public interface LightLevelView {

    /**
     * Returns the level's block registry.
     *
     * @return the block registry
     */
    BlockRegistry registry();

    /**
     * Returns the lowest lit height.
     *
     * @return the minimum height
     */
    int minHeight();

    /**
     * Returns one past the highest lit height.
     *
     * @return the maximum height
     */
    int maxHeight();

    /**
     * Returns whether the level carries sky light.
     *
     * @return true for sky-lit levels
     */
    boolean hasSkyLight();

    /**
     * Returns the loaded chunk, or null when no chunk is loaded there.
     *
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     * @return the chunk, or null when absent
     */
    @Nullable CloudChunk loadedChunk(int chunkX, int chunkZ);
}
