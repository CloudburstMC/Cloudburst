package org.cloudburstmc.server.level.generator.standard.population;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.level.generator.standard.population.cluster.AbstractReplacingPopulator;

import java.util.Objects;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public class SpringPopulator extends AbstractReplacingPopulator {
    public static final Identifier ID = Identifier.fromString("cloudburst:spring");

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
    protected void populate0(PRandom random, ChunkManager level, int blockX, int blockZ) {
        int blockY = this.height.rand(random);

        if (blockY <= 0 || !this.replace.test(level.getBlockAt(blockX, blockY, blockZ, 0))) {
            return;
        }

        final BlockFilter neighbor = this.neighbor;

        int neighbors = 0;
        int air = 0;

        BlockState id = level.getBlockAt(blockX, blockY - 1, blockZ, 0);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockAt(blockX - 1, blockY, blockZ, 0);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockAt(blockX + 1, blockY, blockZ, 0);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockAt(blockX, blockY, blockZ - 1, 0);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }
        id = level.getBlockAt(blockX, blockY, blockZ + 1, 0);
        if (neighbor.test(id)) {
            neighbors++;
        } else if (id == BlockStates.AIR) {
            air++;
        }

        if (this.neighborCount.contains(neighbors) && this.airCount.contains(air)) {
            level.setBlockAt(blockX, blockY, blockZ, 0, this.block.selectWeighted(random));
            //TODO: request immediate block update
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
