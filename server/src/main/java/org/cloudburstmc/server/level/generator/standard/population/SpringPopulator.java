package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
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
public class SpringPopulator extends AbstractReplacingPopulator {
    public static final Identifier ID = Identifier.parse("cloudburst:spring");

    @JsonProperty
    protected IntRange height = IntRange.WHOLE_WORLD;

    @JsonProperty
    protected BlockFilter neighbor = BlockFilter.AIR;

    @JsonProperty
    protected IntRange neighborCount;

    @JsonProperty
    protected IntRange airCount;

    @JsonProperty
    protected BlockSelector block;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.height, "height must be set!");
        Objects.requireNonNull(this.neighbor, "neighbor must be set!");
        Objects.requireNonNull(this.neighborCount, "neighborCount must be set!");
        Objects.requireNonNull(this.airCount, "airCount must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
    }

    @Override
    protected void populate0(RandomGenerator random, GenerationRegion level, int blockX, int blockZ) {
        int blockY = this.height.rand(random);

        if (blockY <= 0 || !this.replace.test(level.getBlockState(blockX, blockY, blockZ))) {
            return;
        }

        final BlockFilter neighbor = this.neighbor;

        int neighbors = 0;
        int air = 0;

        BlockState id = level.getBlockState(blockX, blockY - 1, blockZ);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockState(blockX - 1, blockY, blockZ);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockState(blockX + 1, blockY, blockZ);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockState(blockX, blockY, blockZ - 1);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockState(blockX, blockY, blockZ + 1);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }

        if (this.neighborCount.contains(neighbors) && this.airCount.contains(air)) {
            level.setBlockState(blockX, blockY, blockZ, this.block.selectWeighted(random));
            //TODO: request immediate block update
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
