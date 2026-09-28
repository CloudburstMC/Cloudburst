package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.level.generator.standard.population.cluster.AbstractReplacingPopulator;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import java.util.random.RandomGenerator;

@JsonDeserialize
public class GlowstonePopulator extends AbstractReplacingPopulator {
    public static final Identifier ID = Identifier.parse("cloudburst:glowstone");

    @JsonProperty
    protected IntRange height = IntRange.WHOLE_WORLD;

    @JsonProperty
    protected BlockSelector block;

    @JsonProperty
    protected int tries = 0;

    protected BlockState ground;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.height, "height must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
        Preconditions.checkState(this.tries > 0, "tries must be set!");

        this.ground = generator.ground();
    }

    @Override
    protected void populate0(RandomGenerator random, GenerationRegion level, int blockX, int blockZ) {
        final BlockFilter replace = this.replace;
        final int blockY = this.height.rand(random);
        final BlockState block = this.block.selectWeighted(random);
        final BlockState ground = this.ground;

        if (blockY >= 255 || !replace.test(level.getBlockState(blockX, blockY, blockZ)) || level.getBlockState(blockX, blockY + 1, blockZ) != ground) {
            return;
        }
        level.setBlockState(blockX, blockY, blockZ, block);

        for (int i = this.tries - 1; i >= 0; i--) {
            int x = blockX + random.nextInt(8) - random.nextInt(8);
            int y = blockY - random.nextInt(12);
            int z = blockZ + random.nextInt(8) - random.nextInt(8);

            if (replace.test(level.getBlockState(x, y, z))) {
                int neighbors = 0;
                if (level.getBlockState(x - 1, y, z) == block) {
                    neighbors++;
                }
                if (level.getBlockState(x + 1, y, z) == block) {
                    neighbors++;
                }
                if (level.getBlockState(x, y - 1, z) == block) {
                    neighbors++;
                }
                if (level.getBlockState(x, y + 1, z) == block) {
                    neighbors++;
                }
                if (level.getBlockState(x, y, z - 1) == block) {
                    neighbors++;
                }
                if (level.getBlockState(x, y, z + 1) == block) {
                    neighbors++;
                }

                if (neighbors == 1) {
                    level.setBlockState(x, y, z, block);
                }
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
