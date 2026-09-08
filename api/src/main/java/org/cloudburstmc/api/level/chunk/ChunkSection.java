package org.cloudburstmc.api.level.chunk;

import org.cloudburstmc.api.block.BlockState;

public interface ChunkSection {
    /**
     * Returns the block state at section-local coordinates and storage layer.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the Y coordinate from {@code 0} to {@code 15}
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param layer the storage layer
     * @return the block state
     */
    BlockState getBlockState(int x, int y, int z, int layer);

    /**
     * Replaces the block state at section-local coordinates and storage layer.
     *
     * <p>This mutates section storage directly. Use the level mutation API when
     * block updates, events, or client notifications are required.
     *
     * @param x          the X coordinate from {@code 0} to {@code 15}
     * @param y          the Y coordinate from {@code 0} to {@code 15}
     * @param z          the Z coordinate from {@code 0} to {@code 15}
     * @param layer      the storage layer
     * @param blockState the replacement state
     * @return the state that was previously stored
     */
    BlockState setBlockState(int x, int y, int z, int layer, BlockState blockState);

    byte getSkyLight(int x, int y, int z);

    void setSkyLight(int x, int y, int z, byte value);

    byte getBlockLight(int x, int y, int z);

    void setBlockLight(int x, int y, int z, byte value);

}
