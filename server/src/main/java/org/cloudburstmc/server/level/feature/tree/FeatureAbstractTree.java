package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.level.feature.ReplacingWorldFeature;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import java.util.random.RandomGenerator;

/**
 * Common code for all tree types.
 */
public abstract class FeatureAbstractTree extends ReplacingWorldFeature {
    protected final IntRange height;
    protected final BlockSelector log;
    protected final BlockSelector leaves;

    public FeatureAbstractTree(@NonNull IntRange height) {
        this(height, null, null);
    }

    public FeatureAbstractTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        this(height, new ConstantBlock(species.getLog()), new ConstantBlock(species.getLeaves()));
    }

    public FeatureAbstractTree(@NonNull IntRange height, BlockSelector log, BlockSelector leaves) {
        this.height = height;
        this.log = log;
        this.leaves = leaves;
    }

    @Override
    public boolean place(GenerationRegion level, RandomGenerator random, int x, int y, int z) {
        if (y < 0 || y >= 255) {
            return false;
        }

        final int height = this.chooseHeight(level, random, x, y, z);

        if (!this.canPlace(level, random, x, y, z, height)) {
            return false;
        }

        final BlockState log = this.selectLog(level, random, x, y, z, height);
        final BlockState leaves = this.selectLeaves(level, random, x, y, z, height);

        this.placeLeaves(level, random, x, y, z, height, log, leaves);
        this.placeTrunk(level, random, x, y, z, height, log, leaves);
        this.finish(level, random, x, y, z, height, log, leaves);

        return true;
    }

    protected int chooseHeight(GenerationRegion level, RandomGenerator random, int x, int y, int z) {
        return this.height.rand(random);
    }

    protected abstract boolean canPlace(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height);

    protected BlockState selectLog(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height) {
        return this.log.selectWeighted(random);
    }

    protected BlockState selectLeaves(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height) {
        return this.leaves.selectWeighted(random);
    }

    protected abstract void placeLeaves(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves);

    protected abstract void placeTrunk(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves);

    protected abstract void finish(GenerationRegion level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves);
}
