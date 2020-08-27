package org.cloudburstmc.server.level.generator.standard.generation.decorator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.utils.Identifier;

import static java.lang.Math.min;

/**
 * Places a random number of single blocks on top of a given cover block.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class ScatteredCoverDecorator implements Decorator {
    public static final Identifier ID = Identifier.fromString("cloudburst:scattered_cover");

    @JsonProperty
    protected double chance = Double.NaN;

    @JsonProperty
    protected BlockFilter on;

    @JsonProperty
    protected BlockFilter replace = BlockFilter.AIR;

    @JsonProperty
    protected BlockSelector block;

    @Override
    public void init(long levelSeed, long localSeed, StandardGenerator generator) {
        Preconditions.checkState(this.chance >= 0.0d && this.chance <= 1.0d, "chance must be in range 0-1!");
        Preconditions.checkState(this.on != null, "on must be set!");
        Preconditions.checkState(this.block != null, "block must be set!");
    }

    @Override
    public void decorate(PRandom random, IChunk chunk, int x, int z) {
        BlockFilter on = this.on;
        BlockFilter replace = this.replace;
        double chance = this.chance;
        BlockSelector block = this.block;

        for (int y = min(chunk.getHighestBlock(x, z), 254); y >= 0; y--) {
            if (random.nextDouble() < chance
                    && on.test(chunk.getBlock(x, y, z, 0))
                    && replace.test(chunk.getBlock(x, y + 1, z, 0))) {
                chunk.setBlock(x, y + 1, z, 0, block.selectWeighted(random));
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
