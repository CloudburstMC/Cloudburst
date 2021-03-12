package org.cloudburstmc.api.level;

import com.nukkitx.math.vector.Vector2i;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.math.vector.Vector4i;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;

import java.util.concurrent.CompletableFuture;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface ChunkManager {

    default BlockState getBlockState(Vector3i pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ(), 0);
    }

    default BlockState getBlockState(Vector3i pos, int layer) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ(), layer);
    }

    default BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, 0);
    }

    default BlockState getBlockState(Vector4i pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ(), pos.getW());
    }

    BlockState getBlockState(int x, int y, int z, int layer);

    default Block getBlock(Vector3i pos) {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    Block getBlock(int x, int y, int z);

    default Block getLoadedBlock(Vector3i pos) {
        return getLoadedBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    Block getLoadedBlock(int x, int y, int z);

    default boolean setBlockState(Vector3i pos, BlockState blockState) {
       return this.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, blockState);
    }

    default boolean setBlockState(Vector3i pos, int layer, BlockState blockState) {
        return this.setBlockState(pos.getX(), pos.getY(), pos.getZ(), layer, blockState);
    }

    default boolean setBlockState(Vector3i pos, BlockState blockState, boolean direct) {
        return this.setBlockState(pos, blockState, direct, true);
    }

    default boolean setBlockState(Vector3i pos, int layer, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, direct, update);
    }

    default boolean setBlockState(Vector3i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, blockState, direct, update);
    }

    default boolean setBlockState(Vector4i pos, BlockState blockState) {
        return this.setBlockState(pos, blockState, false);
    }

    default boolean setBlockState(Vector4i pos, BlockState blockState, boolean direct) {
        return this.setBlockState(pos, blockState, direct, true);
    }

    default boolean setBlockState(Vector4i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlockState(pos.getX(), pos.getY(), pos.getZ(), pos.getW(), blockState, direct, update);
    }

    default boolean setBlockState(int x, int y, int z, BlockState state) {
        return setBlockState(x,y,z,0,state,false,true);
    }

    default boolean setBlockState(int x, int y, int z, int layer, BlockState state) {
        return setBlockState(x,y,z,layer,state,false,true);
    }

    boolean setBlockState(int x, int y, int z, int layer, BlockState state, boolean direct, boolean update);

    default Chunk getChunk(Vector3i pos) {
        return getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    default Chunk getChunk(Vector2i chunkPos) {
        return getChunk(chunkPos.getX(), chunkPos.getY());
    }

    Chunk getChunk(int chunkX, int chunkZ);

    Chunk getLoadedChunk(Vector3f position);

    CompletableFuture<Chunk> getChunkFuture(int chunkX, int chunkZ);

    long getSeed();
}
