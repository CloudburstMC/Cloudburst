package org.cloudburstmc.server.level.chunk;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.player.Player;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface IChunk extends Comparable<IChunk> {
    int STATE_NEW = 0;
    int STATE_GENERATED = 1;
    int STATE_POPULATED = 2;
    int STATE_FINISHED = 3;

    @Nonnull
    ChunkSection getOrCreateSection(@Nonnegative int y);

    @Nullable
    ChunkSection getSection(@Nonnegative int y);

    @Nonnull
    ChunkSection[] getSections();

    @Nonnull
    default BlockState getBlock(int x, int y, int z) {
        return this.getBlock(x, y, z, 0);
    }

    @Nonnull
    BlockState getBlock(int x, int y, int z, @Nonnegative int layer);

    default BlockState getAndSetBlock(int x, int y, int z, BlockState blockState) {
        return this.getAndSetBlock(x, y, z, 0, blockState);
    }

    BlockState getAndSetBlock(int x, int y, int z, @Nonnegative int layer, BlockState blockState);

    default void setBlock(int x, int y, int z, BlockState blockState) {
        this.setBlock(x, y, z, 0, blockState);
    }

    void setBlock(int x, int y, int z, @Nonnegative int layer, BlockState blockState);

    int getBiome(int x, int z);

    void setBiome(int x, int z, int biome);

    byte getSkyLight(int x, int y, int z);

    void setSkyLight(int x, int y, int z, @Nonnegative int level);

    byte getBlockLight(int x, int y, int z);

    void setBlockLight(int x, int y, int z, @Nonnegative int level);

    int getHighestBlock(int x, int z);

    void addEntity(@Nonnull Entity entity);

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
    @Nonnull
    Level getLevel();

    /**
     * Get the copy of the biome array.
     *
     * @return biome array
     */
    @Nonnull
    byte[] getBiomeArray();

    /**
     * Get a copy of the height map array.
     *
     * @return height map
     */
    @Nonnull
    int[] getHeightMapArray();

    /**
     * Gets an immutable copy of players currently in this chunk
     *
     * @return player set
     */
    @Nonnull
    Collection<Player> getPlayers();

    /**
     * Gets an immutable copy of entities currently in this chunk
     *
     * @return entity set
     */
    @Nonnull
    Collection<Entity> getEntities();

    /**
     * Gets an immutable copy of all block entities within the current chunk.
     *
     * @return block entity collection
     */
    @Nonnull
    Collection<BlockEntity> getBlockEntities();

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
     */
    default void setDirty() {
        this.setDirty(true);
    }

    /**
     * Sets the chunk's dirty status.
     *
     * @param dirty true if chunk is dirty
     */
    void setDirty(boolean dirty);

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
     * @return this chunk's key, as returned by {@link Chunk#key()}
     */
    default long key() {
        return Chunk.key(this.getX(), this.getZ());
    }

    @Override
    default int compareTo(IChunk o) {
        //compare x positions, and use z position to break ties
        int x = Integer.compare(this.getX(), o.getX());
        return x != 0 ? x : Integer.compare(this.getZ(), o.getZ());
    }
}
