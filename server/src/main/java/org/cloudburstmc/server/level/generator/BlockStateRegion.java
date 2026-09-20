package org.cloudburstmc.server.level.generator;

import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Provides block-state access without exposing live block objects.
 */
public interface BlockStateRegion {

    BlockState getBlockState(int x, int y, int z, BlockLayer layer);

    default BlockState getBlockState(int x, int y, int z) {
        return this.getBlockState(x, y, z, BlockLayer.PRIMARY);
    }

    default BlockState getBlockState(Vector3i position) {
        return this.getBlockState(position.getX(), position.getY(), position.getZ());
    }

    default boolean setBlockState(int x, int y, int z, BlockState state) {
        return this.setBlockState(x, y, z, BlockLayer.PRIMARY, state);
    }

    boolean setBlockState(int x, int y, int z, BlockLayer layer, BlockState state);

    default boolean setBlockState(Vector3i position, BlockState state) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), state);
    }
}
