package org.cloudburstmc.api.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4i;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ChunkManager {

    default BlockState getBlockState(Vector3i position) {
        return getBlockState(position.getX(), position.getY(), position.getZ(), 0);
    }

    default BlockState getBlockState(Vector3i position, int layer) {
        return getBlockState(position.getX(), position.getY(), position.getZ(), layer);
    }

    default BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, 0);
    }

    default BlockState getBlockState(Vector4i position) {
        return getBlockState(position.getX(), position.getY(), position.getZ(), position.getW());
    }

    BlockState getBlockState(int x, int y, int z, int layer);

    default Block getBlock(Vector3i position) {
        return getBlock(position.getX(), position.getY(), position.getZ());
    }

    default Block getBlock(Vector3f position) {
        return getBlock(position.toInt());
    }

    Block getBlock(int x, int y, int z);

    default Block getLoadedBlock(Vector3i position) {
        return getLoadedBlock(position.getX(), position.getY(), position.getZ());
    }

    default Block getLoadedBlock(Vector3f position) {
        return getLoadedBlock(position.toInt());
    }

    Block getLoadedBlock(int x, int y, int z);

    default boolean setBlockState(Vector3i position, BlockState blockState) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), 0, blockState);
    }

    default boolean setBlockState(Vector3i position, int layer, BlockState blockState) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), layer, blockState);
    }

    default boolean setBlockState(Vector3i position, BlockState blockState, boolean direct) {
        return this.setBlockState(position, blockState, direct, true);
    }

    default boolean setBlockState(Vector3i position, int layer, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(position.getX(), position.getY(), position.getZ(), layer, blockState, direct, update);
    }

    default boolean setBlockState(Vector3i position, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(position.getX(), position.getY(), position.getZ(), 0, blockState, direct, update);
    }

    default boolean setBlockState(Vector4i position, BlockState blockState) {
        return this.setBlockState(position, blockState, false);
    }

    default boolean setBlockState(Vector4i position, BlockState blockState, boolean direct) {
        return this.setBlockState(position, blockState, direct, true);
    }

    default boolean setBlockState(Vector4i position, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(position.getX(), position.getY(), position.getZ(), position.getW(), blockState, direct, update);
    }

    default boolean setBlockState(int x, int y, int z, BlockState state) {
        return setBlockState(x, y, z, 0, state, false, true);
    }

    default boolean setBlockState(int x, int y, int z, int layer, BlockState state) {
        return setBlockState(x, y, z, layer, state, false, true);
    }

    boolean setBlockState(int x, int y, int z, int layer, BlockState state, boolean direct, boolean update);

    default Chunk getChunk(Vector3f position) {
        return getChunk(position.toInt());
    }

    default Chunk getChunk(Vector3i position) {
        return getChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    default Chunk getChunk(Vector2i chunkPosition) {
        return getChunk(chunkPosition.getX(), chunkPosition.getY());
    }

    default Chunk getChunk(int chunkX, int chunkZ) {
        return getChunk((((long) chunkX) << 32) | (chunkZ & 0xffffffffL));
    }

    Chunk getChunk(long key);

    @Nullable
    default Chunk getLoadedChunk(Vector3f position) {
        return getLoadedChunk(position.getFloorX() >> 4, position.getFloorZ() >> 4);
    }

    @Nullable
    default Chunk getLoadedChunk(Vector3i position) {
        return getLoadedChunk(position.getX(), position.getZ());
    }

    @Nullable
    default Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return getLoadedChunk((((long) chunkX) << 32) | (chunkZ & 0xffffffffL));
    }

    @Nullable
    Chunk getLoadedChunk(long key);

    CompletableFuture<? extends Chunk> getChunkFuture(int chunkX, int chunkZ);

    long getSeed();

    Set<? extends Chunk> getChunks();

    Set<? extends Player> getChunkPlayers(int chunkX, int chunkZ);

    Set<? extends ChunkLoader> getChunkLoaders(int chunkX, int chunkZ);
}
