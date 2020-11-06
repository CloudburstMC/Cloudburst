package org.cloudburstmc.server.level.feature;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Provides helper methods for other {@link WorldFeature} to quickly check if a block can be replaced.
 *
 * @author DaPorkchop_
 */
public abstract class ReplacingWorldFeature implements WorldFeature, BlockFilter {
    @Override
    public boolean test(BlockState state) {
        Identifier id = state.getId();
        return id == BlockIds.AIR || id == BlockIds.LEAVES || id == BlockIds.LEAVES2 || (!state.getBehavior().isLiquid() && state.getType().isReplaceable());
    }

    public boolean testOrLiquid(BlockState state) {
        Identifier id = state.getId();
        return id == BlockIds.AIR || id == BlockIds.LEAVES || id == BlockIds.LEAVES2 || state.getType().isReplaceable();
    }

    public boolean testOrLiquid(int runtimeId) {
        return runtimeId == 0 || this.testOrLiquid(BlockRegistry.get().getBlock(runtimeId));
    }

    /**
     * Replaces the block at the given coordinates with dirt if it is a grassy block type.
     * <p>
     * The following blocks are considered "grassy":
     * - {@link BlockIds#GRASS}
     * - {@link BlockIds#MYCELIUM}
     * - {@link BlockIds#PODZOL}
     */
    public void replaceGrassWithDirt(ChunkManager level, int x, int y, int z) {
        if (y >= 0 && y < 256) {
            Identifier id = level.getBlockAt(x, y, z).getId();
            if (id == BlockIds.GRASS || id == BlockIds.MYCELIUM || id == BlockIds.PODZOL) {
                level.setBlockAt(x, y, z, BlockStates.DIRT);
            }
        }
    }

    /**
     * Checks whether all the blocks that horizontally neighbor the given coordinates match the given {@link BlockFilter}.
     */
    public boolean allNeighborsMatch(ChunkManager level, int x, int y, int z, BlockFilter filter) {
        return filter.test(level.getBlockAt(x - 1, y, z, 0))
                && filter.test(level.getBlockAt(x + 1, y, z, 0))
                && filter.test(level.getBlockAt(x, y, z - 1, 0))
                && filter.test(level.getBlockAt(x, y, z + 1, 0));
    }

    /**
     * Checks whether all the blocks that horizontally neighbor the given coordinates match the given {@link BlockFilter}.
     */
    public boolean allNeighborsMatch(ChunkManager level, int x, int y, int z, BlockFilter filter, Direction except) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (face != except && !filter.test(level.getBlockAt(x + face.getXOffset(), y, z + face.getZOffset(), 0))) {
                return false;
            }
        }
        return true;
    }
}
