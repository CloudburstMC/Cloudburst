package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import java.util.random.RandomGenerator;

import static net.daporkchop.lib.common.math.PMath.floorI;

/**
 * Generates a huge spruce tree.
 *
 * @author DaPorkchop_
 */
public class FeatureHugeSpruceTree extends FeatureHugeTree {
    public static final IntRange DEFAULT_HEIGHT = new IntRange(13, 28);

    public FeatureHugeSpruceTree(@NonNull IntRange height, @NonNull GenerationTreeSpecies species) {
        super(height, species);
    }

    public FeatureHugeSpruceTree(@NonNull IntRange height, BlockSelector log, BlockSelector leaves) {
        super(height, log, leaves);
    }

    @Override
    protected void placeLeaves(ChunkManager level, RandomGenerator random, int x, int y, int z, int height, BlockState log, BlockState leaves) {
        int heightOffset = this.leafHeightOffset(random, height);
        int lastRadius = 0;
        for (int dy = height - heightOffset; dy <= height; dy++) {
            int radius = floorI(((double) (height - dy) / (double) heightOffset) * 3.5d);
            this.placeCircularLeafLayer(level, x, y + dy, z, radius + (dy > 0 && radius == lastRadius && ((y + dy) & 1) == 0 ? 1 : 0) + 1, leaves);
            lastRadius = radius;
        }
    }

    protected int leafHeightOffset(RandomGenerator random, int height) {
        return random.nextInt(5) + this.height.min;
    }
}
