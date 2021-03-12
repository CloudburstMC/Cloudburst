package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import static java.lang.Math.abs;

/**
 * Generates a normal, vanilla-style tree.
 *
 * @author DaPorkchop_
 */
public class FeatureNormalTree extends FeatureAbstractTree {
    public static final IntRange DEFAULT_HEIGHT = new IntRange(4, 7);

    public FeatureNormalTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureNormalTree(@NonNull IntRange height, @NonNull BlockSelector wood, @NonNull BlockSelector leaves) {
        super(height, wood, leaves);
    }

    @Override
    protected boolean canPlace(ChunkManager level, PRandom random, int x, int y, int z, int height) {
        for (int dy = 0; dy <= height; dy++) {
            if (y + dy < 0 || y + dy >= 256 || !this.test(level.getBlockState(x, y + dy, z, 0))) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void placeLeaves(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        for (int yy = y + height - 3; yy <= y + height; yy++) {
            int dy = yy - (y + height);
            int radius = 1 - (dy / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((abs(dx) != radius || abs(dz) != radius || random.nextBoolean() && dy != 0)
                            && this.test(level.getBlockState(x + dx, yy, z + dz, 0))) {
                        level.setBlockAt(x + dx, yy, z + dz, 0, leaves);
                    }
                }
            }
        }
    }

    @Override
    protected void placeTrunk(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        for (int dy = 0; dy < height; dy++) {
            level.setBlockAt(x, y + dy, z, 0, log);
        }
    }

    @Override
    protected void finish(ChunkManager level, PRandom random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        this.replaceGrassWithDirt(level, x, y - 1, z);
    }
}
