package org.cloudburstmc.server.level.generator.standard.population.tree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Objects;

import static java.lang.Math.abs;

/**
 * Places very short "trees", consisting of only a single log with a pile of leaves around it.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class BushPopulator extends AbstractTreePopulator {
    public static final Identifier ID = Identifier.fromString("cloudburst:bush");

    @JsonProperty
    protected BlockSelector log;

    @JsonProperty
    protected BlockSelector leaves;

    @JsonProperty
    protected int size = 2;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.log, "log must be set!");
        Objects.requireNonNull(this.leaves, "leaves must be set!");
        Preconditions.checkState(this.size > 0, "size must be at least 1!");
    }

    @Override
    protected void placeTree(PRandom random, ChunkManager level, int x, int y, int z) {
        level.setBlockAt(x, ++y, z, 0, this.log.selectWeighted(random));

        final BlockState leaves = this.leaves.selectWeighted(random);
        final int size = this.size;

        for (int dy = 0; dy <= size; dy++) {
            int radius = size - dy;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((abs(dx) != radius || abs(dz) != radius || random.nextBoolean())
                            && this.replace.test(level.getBlockAt(x + dx, y + dy, z + dz, 0))) {
                        level.setBlockAt(x + dx, y + dy, z + dz, 0, leaves);
                    }
                }
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
