package org.cloudburstmc.server.level.chunk;

/**
 * Restores chunk data that requires a constructed chunk instance.
 */
@FunctionalInterface
public interface CloudChunkLoadTask {

    /**
     * Applies the persisted data to a chunk.
     *
     * @param chunk the chunk being loaded
     * @return {@code true} when loading changed data that must be saved
     */
    boolean load(CloudChunk chunk);
}
