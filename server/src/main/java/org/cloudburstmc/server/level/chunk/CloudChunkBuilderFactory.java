package org.cloudburstmc.server.level.chunk;

/**
 * Creates chunk builders bound to a particular level or import operation.
 */
@FunctionalInterface
public interface CloudChunkBuilderFactory {

    /**
     * Creates a builder for the supplied chunk coordinates.
     *
     * @param x the chunk X coordinate
     * @param z the chunk Z coordinate
     * @return a new chunk builder
     */
    CloudChunkBuilder create(int x, int z);
}
