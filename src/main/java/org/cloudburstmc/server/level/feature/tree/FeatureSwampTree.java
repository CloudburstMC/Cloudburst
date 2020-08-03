package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
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

    public FeatureSwampTree(@NonNull IntRange height, @NonNull TreeSpecies species) {
        super(height, species);
    }

    public FeatureSwampTree(@NonNull IntRange height, @NonNull BlockSelector wood, @NonNull BlockSelector leaves) {
        super(height, wood, leaves);
    }

    @Override
    protected void finish(ChunkManager level, PRandom random, int x, int y, int z, int height, int log, int leaves) {
        super.finish(level, random, x, y, z, height, log, leaves);

        y = y + height - 3;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!this.test(org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + dx, y, z + dz, 0))) || random.nextInt(4) != 0) {
                    continue;
                }

                if (org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + dx + 1, y, z + dz, 0)) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.WEST, leaves);
                } else if (org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + dx - 1, y, z + dz, 0)) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.EAST, leaves);
                } else if (org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + dx, y, z + dz + 1, 0)) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.NORTH, leaves);
                } else if (org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x + dx, y, z + dz - 1, 0)) == leaves) {
                    this.placeVines(level, random, x + dx, y, z + dz, Direction.SOUTH, leaves);
                }
            }
        }
    }

    protected void placeVines(ChunkManager level, PRandom random, int x, int y, int z, Direction face, int leaves) {
//        int block = BlockRegistry.get().getRuntimeId(BlockTypes.VINE, BlockBehaviorVine.getMeta(face.getOpposite()));
//        for (int dy = 0, id; dy < 4 && (id = org.cloudburstmc.server.registry.BlockRegistry.get().getRuntimeId(level.getBlockAt(x, y - dy, z, 0))) != leaves && this.test(id); dy++) {
//            level.setBlockRuntimeIdUnsafe(x, y - dy, z, 0, block);
//        }
    }
}
