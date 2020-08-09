package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.Arrays;

/**
 * A very fast chunk implementation, used during initial terrain generation.
 * <p>
 * This class is not thread-safe.
 *
 * @author DaPorkchop_
 */
public final class ChunkPrimer {
    private static final int SECTION_SIZE = 16 * 16 * 16;
    private static final int LAYER_SIZE = SECTION_SIZE * Chunk.SECTION_COUNT;
    private static final int LAYER_COUNT = 2;
    private static final int CHUNK_SIZE = LAYER_SIZE * LAYER_COUNT;

    private static int index(int x, int y, int z, int layer) {
        ChunkSection.checkBounds(x, y, z);
        Preconditions.checkArgument(layer >= 0 && layer < LAYER_COUNT, "layer (%s) is not between 0 and %s", layer, LAYER_COUNT);

        return ChunkSection.blockIndex(x, y, z) | layer * LAYER_SIZE;
    }

    private final char[] data = new char[CHUNK_SIZE];

    public void setBlock(int x, int y, int z, BlockState blockState) {
        this.setBlock(x, y, z, 0, blockState);
    }

    public void setBlock(int x, int y, int z, int layer, BlockState blockState) {
        this.data[index(x, y, z, layer)] = (char) BlockRegistry.get().getRuntimeId(blockState);
    }

    public BlockState getBlock(int x, int y, int z) {
        return this.getBlock(x, y, z, 0);
    }

    public BlockState getBlock(int x, int y, int z, int layer) {
        return BlockRegistry.get().getBlock(this.data[index(x, y, z, layer)]);
    }

    /**
     * Clears this chunk primer, resetting all blocks to air.
     */
    public void clear() {
        Arrays.fill(this.data, (char) 0);
    }
}
