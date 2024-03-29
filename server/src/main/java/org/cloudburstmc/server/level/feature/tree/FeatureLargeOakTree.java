package org.cloudburstmc.server.level.feature.tree;

import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;

import java.util.random.RandomGenerator;

/**
 * Generates either a normal tree or a large oak tree.
 *
 * @author DaPorkchop_
 */
public class FeatureLargeOakTree extends FeatureNormalTree {
    public static final IntRange DEFAULT_HEIGHT = new IntRange(7, 12);

    protected final double chance;

    protected final IntRange largeHeight;

    public FeatureLargeOakTree(@NonNull IntRange normalHeight, @NonNull GenerationTreeSpecies species, double chance, @NonNull IntRange largeHeight) {
        super(normalHeight, species);

        this.chance = chance;
        this.largeHeight = largeHeight;
    }

    public FeatureLargeOakTree(@NonNull IntRange normalHeight, @NonNull BlockSelector wood, @NonNull BlockSelector leaves, double chance, @NonNull IntRange largeHeight) {
        super(normalHeight, wood, leaves);

        this.chance = chance;
        this.largeHeight = largeHeight;
    }

    @Override
    public boolean place(ChunkManager level, RandomGenerator random, int x, int y, int z) {
        if (random.nextDouble() >= this.chance) {
            return super.place(level, random, x, y, z);
        }

        //TODO: actually generate large oak tree
        return super.place(level, random, x, y, z);
    }
}
