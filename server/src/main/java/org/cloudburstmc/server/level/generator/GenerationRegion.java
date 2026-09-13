package org.cloudburstmc.server.level.generator;

import org.cloudburstmc.api.level.chunk.Chunk;

/**
 * A bounded set of locked chunks available to a generation stage.
 *
 * <p>Coordinates are absolute. Access outside the region is rejected instead
 * of loading chunks recursively.
 */
public interface GenerationRegion extends BlockStateRegion {

    long getSeed();

    int getCenterChunkX();

    int getCenterChunkZ();

    int getBuffer();

    boolean containsBlock(int x, int z);

    Chunk getChunk(int chunkX, int chunkZ);
}
