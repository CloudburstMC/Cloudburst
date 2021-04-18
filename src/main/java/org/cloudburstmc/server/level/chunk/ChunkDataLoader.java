package org.cloudburstmc.server.level.chunk;

@FunctionalInterface
public interface ChunkDataLoader {

    /**
     * Loads data into specified chunk.
     *
     * @param chunk chunk
     * @return chunk dirty
     */
    boolean load(CloudChunk chunk);
}
