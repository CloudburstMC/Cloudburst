package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.feature.EndGatewayFeature;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.random.RandomGenerator;

@JsonDeserialize
public class EndGatewayPopulator extends AbstractGenerationPass implements Populator {

    public static final Identifier ID = Identifier.parse("cloudburst:end_gateway");

    @JsonProperty
    private int rarity;

    @JsonProperty
    private IntRange offset;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        Preconditions.checkState(this.rarity > 0, "rarity must be greater than zero");
        Preconditions.checkNotNull(this.offset, "offset must be set");
    }

    @Override
    public void populate(RandomGenerator random, GenerationRegion level, int blockX, int blockZ) {
        if ((blockX & 15) != 0 || (blockZ & 15) != 0 || random.nextInt(this.rarity) != 0) {
            return;
        }

        int localX = random.nextInt(16);
        int localZ = random.nextInt(16);
        int surfaceY = level.getChunk(blockX >> 4, blockZ >> 4).getHighestBlock(localX, localZ);
        if (surfaceY < 0) {
            return;
        }

        Vector3i position = Vector3i.from(blockX + localX, surfaceY + this.offset.rand(random), blockZ + localZ);
        EndGatewayFeature.placeBlocks(level, position);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
