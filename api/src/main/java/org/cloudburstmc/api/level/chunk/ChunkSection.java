package org.cloudburstmc.api.level.chunk;

import org.checkerframework.checker.index.qual.NonNegative;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;

/**
 * Stores block states and lighting for one 16-by-16-by-16 chunk section.
 *
 * <p>All coordinates are local to the section and range from {@code 0} to
 * {@code 15}, inclusive.
 */
public interface ChunkSection {

    /**
     * Returns the primary-layer block state at section-local coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the block state
     */
    default BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, BlockLayer.PRIMARY);
    }

    /**
     * Returns the block state at section-local coordinates in the requested layer.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the Y coordinate from {@code 0} to {@code 15}
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param layer the block layer
     * @return the block state
     */
    BlockState getBlockState(int x, int y, int z, BlockLayer layer);

    /**
     * Replaces the primary-layer block state at section-local coordinates.
     *
     * @param x          the X coordinate
     * @param y          the Y coordinate
     * @param z          the Z coordinate
     * @param blockState the replacement state
     * @return the state that was previously stored
     */
    default BlockState setBlockState(int x, int y, int z, BlockState blockState) {
        return this.setBlockState(x, y, z, BlockLayer.PRIMARY, blockState);
    }

    /**
     * Replaces the block state at section-local coordinates in the requested layer.
     *
     * <p>This mutates section storage directly. Use the level mutation API when
     * block updates, events, or notifications are required.
     *
     * @param x          the X coordinate from {@code 0} to {@code 15}
     * @param y          the Y coordinate from {@code 0} to {@code 15}
     * @param z          the Z coordinate from {@code 0} to {@code 15}
     * @param layer      the block layer
     * @param blockState the replacement state
     * @return the state that was previously stored
     */
    BlockState setBlockState(int x, int y, int z, BlockLayer layer, BlockState blockState);

    /**
     * Returns the sky light level at section-local coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the light level from {@code 0} to {@code 15}
     */
    int getSkyLight(int x, int y, int z);

    /**
     * Sets the sky light level at section-local coordinates.
     *
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @param value the light level from {@code 0} to {@code 15}
     */
    void setSkyLight(int x, int y, int z, @NonNegative int value);

    /**
     * Returns the block light level at section-local coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the light level from {@code 0} to {@code 15}
     */
    int getBlockLight(int x, int y, int z);

    /**
     * Sets the block light level at section-local coordinates.
     *
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @param value the light level from {@code 0} to {@code 15}
     */
    void setBlockLight(int x, int y, int z, @NonNegative int value);
}
