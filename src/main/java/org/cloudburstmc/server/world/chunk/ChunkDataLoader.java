package org.cloudburstmc.server.world.chunk;

@FunctionalInterface
public interface ChunkDataLoader {

    /**
     * Loads data into specified chunk.
     *
     * @param chunk chunk
     * @return chunk dirty
     */
    boolean load(Chunk chunk);
}
