package org.cloudburstmc.server.level.generator.standard.population.tree;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.population.ChancePopulator;

import java.util.Objects;

import static java.lang.Math.min;

/**
 * Base class for all tree populators.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractTreePopulator extends ChancePopulator {
    @JsonProperty
    protected BlockFilter replace = BlockFilter.REPLACEABLE;

    @JsonProperty
    protected BlockFilter on;

    @JsonProperty
    protected IntRange height = IntRange.WHOLE_WORLD;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.replace, "replace must be set!");
        Objects.requireNonNull(this.on, "on must be set!");
        Objects.requireNonNull(this.height, "height must be set!");
    }

    @Override
    public void populate(PRandom random, ChunkManager level, int blockX, int blockZ) {
        final BlockFilter replace = this.replace;
        final BlockFilter on = this.on;

        final int max = min(this.height.max - 1, 254);
        final int min = this.height.min;

        Chunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
        BlockState lastId = chunk.getBlock(blockX & 0xF, max + 1, blockZ & 0xF, 0);
        for (int y = max; y >= min; y--) {
            BlockState id = chunk.getBlock(blockX & 0xF, y, blockZ & 0xF, 0);

            if (replace.test(lastId) && on.test(id) && random.nextDouble() < this.chance) {
                this.placeTree(random, level, blockX, y, blockZ);
            }

            lastId = id;
        }
    }

    protected abstract void placeTree(PRandom random, ChunkManager level, int x, int y, int z);
}
