package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import java.util.random.RandomGenerator;

/**
 * Generates a huge pine tree.
 * <p>
 * Pine trees are identical to spruce trees, but have only a small cluster of leaves near the top.
 *
 * @author DaPorkchop_
 */
public class FeatureHugePineTree extends FeatureHugeSpruceTree {
    public FeatureHugePineTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureHugePineTree(@NonNull IntRange height, BlockSelector log, BlockSelector leaves) {
        super(height, log, leaves);
    }

    @Override
    protected int leafHeightOffset(RandomGenerator random, int height) {
        return random.nextInt(5) + 3;
    }
}
