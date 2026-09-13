package org.cloudburstmc.server.level.generator;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Provides block-state access without exposing live block objects.
 */
public interface BlockStateRegion {

    BlockState getBlockState(int x, int y, int z, int layer);

    BlockState getBlockState(int x, int y, int z);

    BlockState getBlockState(Vector3i position);

    boolean setBlockState(int x, int y, int z, BlockState state);

    boolean setBlockState(int x, int y, int z, int layer, BlockState state);

    boolean setBlockState(Vector3i position, BlockState state);
}
