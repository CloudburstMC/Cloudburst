package org.cloudburstmc.api.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Provides block and chunk access for a level.
 *
 * <p>Methods named {@code getChunk} may load or generate a chunk. Methods
 * named {@code getLoadedChunk} and {@code getLoadedBlock} never load chunks.
 */
public interface ChunkManager {

    /**
     * Returns the primary block state at a position.
     *
     * @param position the block position
     * @return the primary block state
     */
    default BlockState getBlockState(Vector3i position) {
        return this.getBlockState(position.getX(), position.getY(), position.getZ(), BlockLayer.PRIMARY);
    }

    /**
     * Returns the block state in the requested layer.
     *
     * @param position the block position
     * @param layer    the block layer
     * @return the block state
     */
    default BlockState getBlockState(Vector3i position, BlockLayer layer) {
        return this.getBlockState(position.getX(), position.getY(), position.getZ(), layer);
    }

    /**
     * Returns the primary block state at the supplied coordinates.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @return the primary block state
     */
    default BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, BlockLayer.PRIMARY);
    }

    /**
     * Returns the block state in the requested layer.
     *
     * @param x     the block X coordinate
     * @param y     the block Y coordinate
     * @param z     the block Z coordinate
     * @param layer the block layer
     * @return the block state
     */
    BlockState getBlockState(int x, int y, int z, BlockLayer layer);

    /**
     * Returns a live block at a position, loading its chunk when necessary.
     *
     * @param position the block position
     * @return the block
     */
    default Block getBlock(Vector3i position) {
        return getBlock(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Returns a live block at a position, loading its chunk when necessary.
     *
     * @param position the position containing the block
     * @return the block
     */
    default Block getBlock(Vector3f position) {
        return getBlock(position.toInt());
    }

    /**
     * Returns a live block at the supplied coordinates, loading its chunk when necessary.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @return the block
     */
    Block getBlock(int x, int y, int z);

    /**
     * Returns a live block without loading its chunk.
     *
     * @param position the block position
     * @return the block, or {@code null} when its chunk is not loaded
     */
    @Nullable
    default Block getLoadedBlock(Vector3i position) {
        return getLoadedBlock(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Returns a live block without loading its chunk.
     *
     * @param position the position containing the block
     * @return the block, or {@code null} when its chunk is not loaded
     */
    @Nullable
    default Block getLoadedBlock(Vector3f position) {
        return getLoadedBlock(position.toInt());
    }

    /**
     * Returns a live block without loading its chunk.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @return the block, or {@code null} when its chunk is not loaded
     */
    @Nullable
    Block getLoadedBlock(int x, int y, int z);

    /**
     * Replaces the primary state at a position and runs normal updates.
     *
     * @param position the block position
     * @param blockState the replacement state
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(Vector3i position, BlockState blockState) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), BlockLayer.PRIMARY, blockState);
    }

    /**
     * Replaces a state at a position and runs normal updates.
     *
     * @param position the block position
     * @param layer the block layer to replace
     * @param blockState the replacement state
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(Vector3i position, BlockLayer layer, BlockState blockState) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), layer, blockState);
    }

    /**
     * Replaces the primary state at a position.
     *
     * @param position the block position
     * @param blockState the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(Vector3i position, BlockState blockState, boolean direct) {
        return this.setBlockState(position, blockState, direct, true);
    }

    /**
     * Replaces a state at a position.
     *
     * @param position the block position
     * @param layer the block layer to replace
     * @param blockState the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, entities, liquids, and neighboring blocks
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(Vector3i position, BlockLayer layer, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(position.getX(), position.getY(), position.getZ(), layer, blockState, direct, update);
    }

    /**
     * Replaces the primary state at a position.
     *
     * @param position the block position
     * @param blockState the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, entities, liquids, and neighboring blocks
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(Vector3i position, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(position.getX(), position.getY(), position.getZ(), BlockLayer.PRIMARY, blockState, direct, update);
    }

    /**
     * Replaces the primary state at the supplied coordinates and runs normal updates.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @param state the replacement state
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(int x, int y, int z, BlockState state) {
        return setBlockState(x, y, z, BlockLayer.PRIMARY, state, false, true);
    }

    /**
     * Replaces the primary state at the supplied coordinates.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @param state the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, entities, liquids, and neighboring blocks
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(int x, int y, int z, BlockState state, boolean direct, boolean update) {
        return this.setBlockState(x, y, z, BlockLayer.PRIMARY, state, direct, update);
    }

    /**
     * Replaces a state at the supplied coordinates and runs normal updates.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @param layer the block layer to replace
     * @param state the replacement state
     * @return {@code true} when the stored state changed
     */
    default boolean setBlockState(int x, int y, int z, BlockLayer layer, BlockState state) {
        return setBlockState(x, y, z, layer, state, false, true);
    }

    /**
     * Replaces a block state in the requested layer.
     *
     * @param x      the block X coordinate
     * @param y      the block Y coordinate
     * @param z      the block Z coordinate
     * @param layer  the block layer to replace
     * @param state  the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, entities, liquids, and neighboring blocks
     * @return {@code true} when the stored state changed
     */
    boolean setBlockState(int x, int y, int z, BlockLayer layer, BlockState state, boolean direct, boolean update);

    /**
     * Returns the chunk containing a position, loading it when necessary.
     *
     * @param position the position within the chunk
     * @return the chunk
     */
    default Chunk getChunk(Vector3f position) {
        return getChunk(position.toInt());
    }

    /**
     * Returns the chunk containing a block position, loading it when necessary.
     *
     * @param position the block position
     * @return the chunk
     */
    default Chunk getChunk(Vector3i position) {
        return getChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    /**
     * Returns a chunk by its chunk coordinates, loading it when necessary.
     *
     * @param chunkPosition the chunk coordinates, with X and Z stored as the vector's X and Y values
     * @return the chunk
     */
    default Chunk getChunk(Vector2i chunkPosition) {
        return getChunk(chunkPosition.getX(), chunkPosition.getY());
    }

    /**
     * Returns a chunk by its coordinates, loading it when necessary.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the chunk
     */
    default Chunk getChunk(int chunkX, int chunkZ) {
        return getChunk((((long) chunkX) << 32) | (chunkZ & 0xffffffffL));
    }

    /**
     * Returns a chunk by its packed coordinate key, loading it when necessary.
     *
     * @param key the packed chunk coordinate key
     * @return the chunk
     */
    Chunk getChunk(long key);

    /**
     * Returns the loaded chunk containing a position without loading it.
     *
     * @param position the position within the chunk
     * @return the chunk, or {@code null} when it is not loaded
     */
    @Nullable
    default Chunk getLoadedChunk(Vector3f position) {
        return getLoadedChunk(position.getFloorX() >> 4, position.getFloorZ() >> 4);
    }

    /**
     * Returns the loaded chunk containing a block position without loading it.
     *
     * @param position the block position
     * @return the chunk, or {@code null} when it is not loaded
     */
    @Nullable
    default Chunk getLoadedChunk(Vector3i position) {
        return getLoadedChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    /**
     * Returns a loaded chunk by its coordinates without loading it.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the chunk, or {@code null} when it is not loaded
     */
    @Nullable
    default Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return getLoadedChunk((((long) chunkX) << 32) | (chunkZ & 0xffffffffL));
    }

    /**
     * Returns a loaded chunk by its packed coordinate key without loading it.
     *
     * @param key the packed chunk coordinate key
     * @return the chunk, or {@code null} when it is not loaded
     */
    @Nullable
    Chunk getLoadedChunk(long key);

    /**
     * Loads a chunk asynchronously.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return a future completed with the chunk
     */
    CompletableFuture<? extends Chunk> getChunkFuture(int chunkX, int chunkZ);

    /**
     * Returns the level seed used by this manager.
     *
     * @return the level seed
     */
    long getSeed();

    /**
     * Returns an unmodifiable snapshot of the currently loaded chunks.
     *
     * @return the loaded chunks
     */
    Set<? extends Chunk> getChunks();

    /**
     * Returns an unmodifiable snapshot of players viewing a chunk.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the players viewing the chunk
     */
    Set<? extends Player> getChunkPlayers(int chunkX, int chunkZ);
}
