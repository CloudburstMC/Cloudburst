package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.feature.EndIslandFeature;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import java.util.random.RandomGenerator;

@JsonDeserialize
public class EndIslandPopulator extends ChancePopulator.Column {
    public static final Identifier ID = Identifier.parse("cloudburst:end_island");

    @JsonProperty
    protected IntRange height;

    @JsonProperty
    protected BlockSelector block;

    @JsonProperty
    protected IntRange radius;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.height, "height must be set!");
        Objects.requireNonNull(this.block, "block must be set!");
        Objects.requireNonNull(this.radius, "radius must be set!");
    }

    @Override
    protected void populate0(RandomGenerator random, GenerationRegion level, int blockX, int blockZ) {
        final int blockY = this.height.rand(random);
        final BlockState block = this.block.selectWeighted(random);
        double radius = this.radius.rand(random);
        EndIslandFeature.place(level, Vector3i.from(blockX, blockY, blockZ), block, random, radius);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
