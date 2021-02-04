package org.cloudburstmc.api.level.chunk;

import org.cloudburstmc.api.block.BlockState;

public interface ChunkSection {
    BlockState getBlock(int x, int y, int z, int layer);

    void setBlock(int x, int y, int z, int layer, BlockState blockState);

    byte getSkyLight(int x, int y, int z);

    void setSkyLight(int x, int y, int z, byte value);

    byte getBlockLight(int x, int y, int z);

    void setBlockLight(int x, int y, int z, byte value);

}
