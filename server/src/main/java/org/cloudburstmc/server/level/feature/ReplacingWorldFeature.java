package org.cloudburstmc.server.level.feature;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * Provides helper methods for other {@link WorldFeature} to quickly check if a block can be replaced.
 *
 * @author DaPorkchop_
 */
public abstract class ReplacingWorldFeature implements WorldFeature, BlockFilter {
    @Override
    public boolean test(BlockState state) {
        Identifier id = state.getType().getId();

        return id == BlockIds.AIR || id == BlockIds.LEAVES || id == BlockIds.LEAVES2 ||
                (!CloudBlockRegistry.REGISTRY.getBehavior(state.getType(), BlockBehaviors.IS_LIQUID) &&
                        CloudBlockRegistry.REGISTRY.getBehavior(state.getType(), BlockBehaviors.IS_REPLACEABLE));
    }

    public boolean testOrLiquid(BlockState state) {
        BlockType type = state.getType();
        return type == BlockTypes.AIR || type == BlockTypes.LEAVES ||
                CloudBlockRegistry.REGISTRY.getBehavior(state.getType(), BlockBehaviors.IS_REPLACEABLE);
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
            Identifier id = level.getBlockState(x, y, z).getType().getId();
            if (id == BlockIds.GRASS || id == BlockIds.MYCELIUM || id == BlockIds.PODZOL) {
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
            if (face != except && !filter.test(level.getBlockState(x + face.getXOffset(), y, z + face.getZOffset(), 0))) {
                return false;
            }
        }
        return true;
    }
}
