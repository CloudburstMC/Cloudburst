package org.cloudburstmc.server.level.generator.standard.population.plant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.population.cluster.AbstractReplacingPopulator;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public abstract class AbstractPlantPopulator extends AbstractReplacingPopulator {
    @JsonProperty
    protected BlockFilter on;

    @JsonProperty
    protected int patchSize = 64;

    public AbstractPlantPopulator() {
        this.replace = BlockFilter.AIR;
    }

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.on, "on must be set!");
        Preconditions.checkState(this.patchSize > 0, "patchSize must be at least 1!");
    }

    @Override
    protected void populate0(RandomGenerator random, ChunkManager level, int blockX, int blockZ) {
        int height = level.getChunk(blockX >> 4, blockZ >> 4).getHighestBlock(blockX & 0xF, blockZ & 0xF);
        this.placeCluster(random, level, blockX, random.nextInt(height << 1), blockZ);
    }

    protected abstract void placeCluster(RandomGenerator random, ChunkManager level, int x, int y, int z);
}
