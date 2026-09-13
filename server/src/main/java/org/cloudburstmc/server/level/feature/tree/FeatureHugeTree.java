package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import java.util.random.RandomGenerator;

/**
 * Common code for all huge (2x2) tree types.
 */
public abstract class FeatureHugeTree extends FeatureAbstractTree {
    public FeatureHugeTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureHugeTree(@NonNull IntRange height, BlockSelector log, BlockSelector leaves) {
        super(height, log, leaves);
    }

    @Override
    public boolean place(GenerationRegion level, RandomGenerator random, int x, int y, int z) {
        final int height = this.height.rand(random);

        if (!this.canPlace(level, random, x, y, z, height)) {
            return false;
        }

        final BlockState log = this.log.selectWeighted(random);
        final BlockState leaves = this.leaves.selectWeighted(random);

        this.placeLeaves(level, random, x, y, z, height, log, leaves);
        this.placeTrunk(level, random, x, y, z, height, log, leaves);
        this.finish(level, random, x, y, z, height, log, leaves);

        return true;
    }

    @Override
    protected boolean canPlace(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height) {
        for (int dy = 0; dy <= height + 1; dy++) {
            if (y + dy < 0 || y + dy >= 256) {
                return false;
            }
            int radius = dy == 0 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!this.test(level.getBlockState(x + dx, y + dy, z + dz, 0))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected void placeTrunk(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        for (int dy = 0; dy < height - 2; dy++) {
            level.setBlockState(x, y + dy, z, 0, log);
            level.setBlockState(x + 1, y + dy, z, 0, log);
            level.setBlockState(x, y + dy, z + 1, 0, log);
            level.setBlockState(x + 1, y + dy, z + 1, 0, log);
        }
    }

    @Override
    protected void finish(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        this.replaceGrassWithDirt(level, x, y - 1, z);
        this.replaceGrassWithDirt(level, x + 1, y - 1, z);
        this.replaceGrassWithDirt(level, x, y - 1, z + 1);
        this.replaceGrassWithDirt(level, x + 1, y - 1, z + 1);
    }

    protected void placeCircularLeafLayer(GenerationRegion level, int x, int y, int z, int radius, BlockState block) {
        if (y < 0 || y >= 256) {
            return;
        }

        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius + 1; dx++) {
            for (int dz = -radius; dz <= radius + 1; dz++) {
                int dxSq = dx > 0 ? (dx - 1) * (dx - 1) : dx * dx;
                int dzSq = dz > 0 ? (dz - 1) * (dz - 1) : dz * dz;
                if (dxSq + dzSq <= radiusSq && this.test(level.getBlockState(x + dx, y, z + dz, 0))) {
                    level.setBlockState(x + dx, y, z + dz, 0, block);
                }
            }
        }
    }
}
