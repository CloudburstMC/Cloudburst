package org.cloudburstmc.server.level.feature;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;

/**
 * Provides helper methods for other {@link WorldFeature} to quickly check if a block can be replaced.
 */
public abstract class ReplacingWorldFeature implements WorldFeature, BlockFilter {
    @Override
    public boolean test(BlockState state) {
        BlockType type = state.getType();
        return type == BlockTypes.AIR || state.is(BlockTags.LEAVES) || (!type.isLiquid() && state.isReplaceable());
    }

    public boolean testOrLiquid(BlockState state) {
        BlockType type = state.getType();
        return type == BlockTypes.AIR || type.is(BlockTags.LEAVES) || state.isReplaceable();
    }

    /**
     * Replaces the block at the given coordinates with dirt if it is a grassy block type.
     * <p>
     * The following blocks are considered "grassy":
     * - {@link BlockTypes#GRASS_BLOCK}
     * - {@link BlockTypes#MYCELIUM}
     * - {@link BlockTypes#PODZOL}
     */
    public void replaceGrassWithDirt(ChunkManager level, int x, int y, int z) {
        if (y >= 0 && y < 256) {
            BlockType type = level.getBlockState(x, y, z).getType();
            if (type == BlockTypes.GRASS_BLOCK || type == BlockTypes.MYCELIUM || type == BlockTypes.PODZOL) {
                level.setBlockState(x, y, z, BlockStates.DIRT);
            }
        }
    }

    /**
     * Checks whether all the blocks that horizontally neighbor the given coordinates match the given {@link BlockFilter}.
     */
    public boolean allNeighborsMatch(ChunkManager level, int x, int y, int z, BlockFilter filter) {
        return filter.test(level.getBlockState(x - 1, y, z, 0))
                && filter.test(level.getBlockState(x + 1, y, z, 0))
                && filter.test(level.getBlockState(x, y, z - 1, 0))
                && filter.test(level.getBlockState(x, y, z + 1, 0));
    }

    /**
     * Checks whether all the blocks that horizontally neighbor the given coordinates match the given {@link BlockFilter}.
     */
    public boolean allNeighborsMatch(ChunkManager level, int x, int y, int z, BlockFilter filter, Direction except) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (face != except && !filter.test(level.getBlockState(x + face.getStepX(), y, z + face.getStepZ(), 0))) {
                return false;
            }
        }

        return true;
    }
}
