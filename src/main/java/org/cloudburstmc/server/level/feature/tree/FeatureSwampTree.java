package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.math.Direction;

/**
 * Generates normal trees, but with vines on the sides.
 *
 * @author DaPorkchop_
 */
public class FeatureSwampTree extends FeatureNormalTree {
    public static final IntRange DEFAULT_HEIGHT = new IntRange(5, 8);

    public FeatureSwampTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureSwampTree(@NonNull IntRange height, @NonNull BlockSelector wood, @NonNull BlockSelector leaves) {
        super(height, wood, leaves);
    }

    @Override
    protected void finish(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        super.finish(level, random, x, y, z, height, log, leaves);

        y = y + height - 3;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!this.test(level.getBlockAt(x + dx, y, z + dz, 0)) || random.nextInt(4) != 0) {
                    continue;
                }

                if (level.getBlockAt(x + dx + 1, y, z + dz, 0) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.WEST, leaves);
                } else if (level.getBlockAt(x + dx - 1, y, z + dz, 0) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.EAST, leaves);
                } else if (level.getBlockAt(x + dx, y, z + dz + 1, 0) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.NORTH, leaves);
                } else if (level.getBlockAt(x + dx, y, z + dz - 1, 0) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.SOUTH, leaves);
                }
            }
        }
    }

    protected void placeVines(ChunkManager level, PRandom random, int x, int y, int z, Direction face, BlockState leaves) {
        BlockState vine = BlockStates.VINE.withTrait(BlockTraits.FACING_DIRECTION, face.getOpposite());
        BlockState block;
        for (int dy = 0; dy < 4 && (block = level.getBlockAt(x, y - dy, z, 0)) != leaves && this.test(block); dy++) {
            level.setBlockAt(x, y - dy, z, 0, vine);
        }
    }
}
