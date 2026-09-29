package org.cloudburstmc.server.level.generator.standard.population.plant;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Places patches of plants of varying heights in the world.
 */
@JsonDeserialize
public class PlantPopulator extends AbstractPlantPopulator {
    public static final Identifier ID = Identifier.parse("cloudburst:plant");

    @JsonProperty
    protected BlockSelector block;

    @JsonProperty
    protected BlockFilter water;

    @JsonProperty
    protected IntRange height;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.block, "block must be set!");
        Objects.requireNonNull(this.height, "height must be set!");
    }

    @Override
    protected void placeCluster(RandomGenerator random, GenerationRegion level, int x, int y, int z) {
        final BlockFilter on = this.on;
        final BlockFilter water = this.water;
        final BlockFilter replace = this.replace;
        final BlockState block = this.block.selectWeighted(random);

        for (int i = this.patchSize - 1; i >= 0; i--) {
            int blockY = y + random.nextInt(4) - random.nextInt(4);
            int height = this.height.rand(random);
            if (blockY < 0 || blockY >= 256 - height) {
                continue;
            }
            int blockX = x + random.nextInt(8) - random.nextInt(8);
            int blockZ = z + random.nextInt(8) - random.nextInt(8);

            Chunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
            if (!on.test(chunk.getBlockState(blockX & 0xF, blockY, blockZ & 0xF))) {
                continue;
            }
            if (water != null && !(water.test(level.getBlockState(blockX + 1, blockY, blockZ))
                    || water.test(level.getBlockState(blockX - 1, blockY, blockZ))
                    || water.test(level.getBlockState(blockX, blockY, blockZ + 1))
                    || water.test(level.getBlockState(blockX, blockY, blockZ - 1)))) {
                continue;
            }
            for (int dy = 1; dy <= height && replace.test(chunk.getBlockState(blockX & 0xF, blockY + dy, blockZ & 0xF))
                    && replace.test(level.getBlockState(blockX + 1, blockY + dy, blockZ))
                    && replace.test(level.getBlockState(blockX - 1, blockY + dy, blockZ))
                    && replace.test(level.getBlockState(blockX, blockY + dy, blockZ + 1))
                    && replace.test(level.getBlockState(blockX, blockY + dy, blockZ - 1)); dy++) {
                chunk.setBlockState(blockX & 0xF, blockY + dy, blockZ & 0xF, block);
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
