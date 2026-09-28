package org.cloudburstmc.api.level.chunk;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;

import java.util.Set;

/**
 * A loaded 16-by-16 column of block, biome, lighting, and entity data.
 *
 * <p>Block coordinates are local to the chunk on the X and Z axes. Y
 * coordinates are absolute level coordinates unless stated otherwise.
 */
public interface Chunk extends Comparable<Chunk> {

    /**
     * Returns the chunk's X coordinate.
     *
     * @return the chunk X coordinate
     */
    int getX();

    /**
     * Returns the chunk's Z coordinate.
     *
     * @return the chunk Z coordinate
     */
    int getZ();

    /**
     * Returns the level containing this chunk.
     *
     * @return the level
     */
    Level getLevel();

    /**
     * Returns the chunk coordinates packed into a long.
     *
     * @return the packed chunk key
     */
    default long getKey() {
        return ((long) this.getX() << 32) | (this.getZ() & 0xffffffffL);
    }

    @Override
    default int compareTo(Chunk other) {
        int x = Integer.compare(this.getX(), other.getX());
        return x != 0 ? x : Integer.compare(this.getZ(), other.getZ());
    }

    /**
     * Checks whether terrain has been generated for this chunk.
     *
     * @return {@code true} when terrain has been generated
     */
    boolean isGenerated();

    /**
     * Returns a section by array index, creating it when absent.
     *
     * @param index the section index from {@code 0} to the level's section count minus one
     * @return the section
     */
    ChunkSection getOrCreateSection(@NonNegative int index);

    /**
     * Returns a section by array index without creating it.
     *
     * @param index the section index from {@code 0} to the level's section count minus one
     * @return the section, or {@code null} when it has not been allocated
     */
    @Nullable
    ChunkSection getSection(@NonNegative int index);

    /**
     * Returns a snapshot of the section array.
     *
     * <p>Changing the returned array does not affect this chunk. The array may
     * contain {@code null} entries for unallocated sections, and its section
     * objects remain live and mutable.
     *
     * @return the sections indexed from the level's minimum section
     */
    ChunkSection[] getSections();

    /**
     * Returns the primary-layer block state at chunk-local coordinates.
     *
     * @param x the X coordinate from {@code 0} to {@code 15}
     * @param y the absolute Y coordinate
     * @param z the Z coordinate from {@code 0} to {@code 15}
     * @return the block state
     */
    default BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, BlockLayer.PRIMARY);
    }

    /**
     * Returns the block state at chunk-local coordinates in the requested layer.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the absolute Y coordinate
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param layer the block layer
     * @return the block state
     */
    BlockState getBlockState(int x, int y, int z, BlockLayer layer);

    /**
     * Replaces the primary-layer block state at chunk-local coordinates.
     *
     * @param x          the X coordinate from {@code 0} to {@code 15}
     * @param y          the absolute Y coordinate
     * @param z          the Z coordinate from {@code 0} to {@code 15}
     * @param blockState the replacement state
     * @return the state that was previously stored
     */
    default BlockState setBlockState(int x, int y, int z, BlockState blockState) {
        return this.setBlockState(x, y, z, BlockLayer.PRIMARY, blockState);
    }

    /**
     * Replaces the block state at chunk-local coordinates in the requested layer.
     *
     * <p>This mutates chunk storage directly. Use the level mutation API when
     * block updates, events, or notifications are required.
     *
     * @param x          the X coordinate from {@code 0} to {@code 15}
     * @param y          the absolute Y coordinate
     * @param z          the Z coordinate from {@code 0} to {@code 15}
     * @param layer      the block layer
     * @param blockState the replacement state
     * @return the state that was previously stored
     */
    BlockState setBlockState(int x, int y, int z, BlockLayer layer, BlockState blockState);

    /**
     * Returns the biome ID at chunk-local coordinates.
     *
     * @param x the X coordinate from {@code 0} to {@code 15}
     * @param y the absolute Y coordinate
     * @param z the Z coordinate from {@code 0} to {@code 15}
     * @return the biome ID
     */
    int getBiome(int x, int y, int z);

    /**
     * Replaces the biome ID at chunk-local coordinates.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the absolute Y coordinate
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param biome the biome ID
     */
    void setBiome(int x, int y, int z, int biome);

    /**
     * Sets the biome ID throughout one chunk-local XZ column.
     *
     * @param x       the X coordinate from {@code 0} to {@code 15}
     * @param z       the Z coordinate from {@code 0} to {@code 15}
     * @param biomeId the biome ID
     */
    default void fillColumnBiome(int x, int z, int biomeId) {
        for (int y = this.getLevel().getMinHeight(); y < this.getLevel().getMaxHeight(); y++) {
            this.setBiome(x, y, z, biomeId);
        }
    }

    /**
     * Returns the sky light level at chunk-local coordinates.
     *
     * @param x the X coordinate from {@code 0} to {@code 15}
     * @param y the absolute Y coordinate
     * @param z the Z coordinate from {@code 0} to {@code 15}
     * @return the light level from {@code 0} to {@code 15}
     */
    int getSkyLight(int x, int y, int z);

    /**
     * Sets the sky light level at chunk-local coordinates.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the absolute Y coordinate
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param level the light level from {@code 0} to {@code 15}
     */
    void setSkyLight(int x, int y, int z, @NonNegative int level);

    /**
     * Returns the block light level at chunk-local coordinates.
     *
     * @param x the X coordinate from {@code 0} to {@code 15}
     * @param y the absolute Y coordinate
     * @param z the Z coordinate from {@code 0} to {@code 15}
     * @return the light level from {@code 0} to {@code 15}
     */
    int getBlockLight(int x, int y, int z);

    /**
     * Sets the block light level at chunk-local coordinates.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the absolute Y coordinate
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param level the light level from {@code 0} to {@code 15}
     */
    void setBlockLight(int x, int y, int z, @NonNegative int level);

    /**
     * Returns the highest non-air block in a chunk column.
     *
     * @param x the X coordinate from {@code 0} to {@code 15}
     * @param z the Z coordinate from {@code 0} to {@code 15}
     * @return the absolute Y coordinate, or {@code -1} when the column is empty
     */
    int getHighestBlock(int x, int z);

    /**
     * Returns a copy of the chunk height map.
     *
     * @return the height map indexed by {@code z * 16 + x}
     */
    int[] getHeightMap();

    /**
     * Returns the block entity at chunk-local coordinates.
     *
     * @param x the X coordinate from {@code 0} to {@code 15}
     * @param y the absolute Y coordinate
     * @param z the Z coordinate from {@code 0} to {@code 15}
     * @return the block entity, or {@code null} when none is present
     */
    @Nullable
    BlockEntity getBlockEntity(int x, int y, int z);

    /**
     * Returns an unmodifiable snapshot of players whose positions are in this chunk.
     *
     * @return the players in the chunk
     */
    Set<? extends Player> getPlayers();

    /**
     * Returns an unmodifiable snapshot of non-player entities in this chunk.
     *
     * @return the entities in the chunk
     */
    Set<? extends Entity> getEntities();

    /**
     * Returns an unmodifiable snapshot of block entities in this chunk.
     *
     * @return the block entities in the chunk
     */
    Set<? extends BlockEntity> getBlockEntities();

    /**
     * Returns an unmodifiable snapshot of players viewing this chunk.
     *
     * @return the chunk viewers
     */
    Set<? extends Player> getViewers();
}
