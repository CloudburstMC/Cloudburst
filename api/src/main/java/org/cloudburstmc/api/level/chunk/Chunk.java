package org.cloudburstmc.api.level.chunk;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.ChunkLoader;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;

import java.util.Set;

public interface Chunk extends Comparable<Chunk> {
    int STATE_NEW = 0;
    int STATE_GENERATED = 1;
    int STATE_POPULATED = 2;
    int STATE_FINISHED = 3;

    ChunkSection getOrCreateSection(@NonNegative int y);

    @Nullable
    ChunkSection getSection(@NonNegative int y);

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
        return this.getBlockState(x, y, z, 0);
    }

    /**
     * Returns the block state at chunk-local coordinates and storage layer.
     *
     * @param x     the X coordinate from {@code 0} to {@code 15}
     * @param y     the absolute Y coordinate
     * @param z     the Z coordinate from {@code 0} to {@code 15}
     * @param layer the storage layer
     * @return the block state
     */
    BlockState getBlockState(int x, int y, int z, @NonNegative int layer);

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
        return this.setBlockState(x, y, z, 0, blockState);
    }

    /**
     * Replaces the block state at chunk-local coordinates and storage layer.
     *
     * <p>This mutates chunk storage directly. Use the level mutation API when
     * block updates, events, or client notifications are required.
     *
     * @param x          the X coordinate from {@code 0} to {@code 15}
     * @param y          the absolute Y coordinate
     * @param z          the Z coordinate from {@code 0} to {@code 15}
     * @param layer      the storage layer
     * @param blockState the replacement state
     * @return the state that was previously stored
     */
    BlockState setBlockState(int x, int y, int z, @NonNegative int layer, BlockState blockState);

    int getBiome(int x, int y, int z);

    void setBiome(int x, int y, int z, int biome);

    /**
     * Sets the biome ID for every Y coordinate in a single XZ column,
     * spanning the full build height of the level.
     *
     * <p>This is faster than calling {@link #setBiome} in a loop because
     * implementations can delegate to section-level bulk helpers.
     * The default implementation falls back to a plain loop.
     *
     * @param x       0–15 within the chunk
     * @param z       0–15 within the chunk
     * @param biomeId raw biome integer ID
     */
    default void fillColumnBiome(int x, int z, int biomeId) {
        int minY = getLevel().getMinHeight();
        int maxY = getLevel().getMaxHeight();
        for (int y = minY; y < maxY; y++) {
            setBiome(x, y, z, biomeId);
        }
    }

    byte getSkyLight(int x, int y, int z);

    void setSkyLight(int x, int y, int z, @NonNegative int level);

    byte getBlockLight(int x, int y, int z);

    void setBlockLight(int x, int y, int z, @NonNegative int level);

    int getHighestBlock(int x, int z);

    void addEntity(@NonNull Entity entity);

    void removeEntity(Entity entity);

    void addBlockEntity(BlockEntity blockEntity);

    void removeBlockEntity(BlockEntity blockEntity);

    BlockEntity getBlockEntity(int x, int y, int z);

    /**
     * Get the chunk's X coordinate in the level it was loaded.
     *
     * @return chunk x
     */
    int getX();

    /**
     * Get the chunk's Z coordinate in the level it was loaded.
     *
     * @return chunk z
     */
    int getZ();

    /**
     * Get the level the chunk was loaded in.
     *
     * @return chunk level
     */
    Level getLevel();

    /**
     * Get a copy of the height map array.
     *
     * @return height map
     */
    int[] getHeightMapArray();

    /**
     * Gets an immutable copy of players currently in this chunk
     *
     * @return player set
     */
    Set<? extends Player> getPlayers();

    /**
     * Gets an immutable copy of entities currently in this chunk
     *
     * @return entity set
     */
    Set<? extends Entity> getEntities();

    /**
     * Gets an immutable copy of all block entities within the current chunk.
     *
     * @return block entity set
     */
    Set<? extends BlockEntity> getBlockEntities();

    /**
     * Gets this chunk's current state.
     */
    int getState();

    /**
     * Atomically updates this chunk's state.
     *
     * @param next the new state to set
     * @return the chunk's previous state
     * @throws IllegalStateException if the new state is invalid, or the same as or lower than the current state
     */
    int setState(int next);

    default boolean isGenerated() {
        return this.getState() >= STATE_GENERATED;
    }

    default boolean isPopulated() {
        return this.getState() >= STATE_POPULATED;
    }

    default boolean isFinished() {
        return this.getState() >= STATE_FINISHED;
    }

    /**
     * Whether the chunk has changed since it was last loaded or saved.
     *
     * @return dirty
     */
    boolean isDirty();

    /**
     * Sets the chunk's dirty status.
     *
     * @param dirty true if chunk is dirty
     */
    void setDirty(boolean dirty);

    /**
     * Sets the chunk's dirty status.
     */
    default void setDirty() {
        this.setDirty(true);
    }

    /**
     * Atomically resets this chunk's dirty status.
     *
     * @return whether or not the chunk was previously dirty
     */
    boolean clearDirty();

    /**
     * Clear chunk to a state as if it was not generated.
     */
    void clear();

    /**
     * @return this chunk's key
     */
    default long key() {
        return (((long) getX()) << 32) | (getZ() & 0xffffffffL);
    }

    @Override
    default int compareTo(Chunk o) {
        //compare x positions, and use z position to break ties
        int x = Integer.compare(this.getX(), o.getX());
        return x != 0 ? x : Integer.compare(this.getZ(), o.getZ());
    }

    LockableChunk readLockable();

    LockableChunk writeLockable();

    void close();

    Set<? extends ChunkLoader> getLoaders();

    Set<? extends Player> getViewers();

}
