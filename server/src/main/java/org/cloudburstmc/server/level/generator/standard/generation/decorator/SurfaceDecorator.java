package org.cloudburstmc.server.level.generator.standard.generation.decorator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;

/**
 * Places the surface blocks on terrain, consisting of a single "top" block followed by a number of "filler" blocks.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class SurfaceDecorator extends DepthNoiseDecorator {
    public static final Identifier ID = Identifier.fromString("cloudburst:surface");

    @JsonProperty
    protected IntRange height = null;

    @JsonProperty
    protected BlockState ground = null;
    @JsonProperty
    protected BlockState cover = null;
    @JsonProperty
    protected BlockState top = null;
    @JsonProperty
    protected BlockState filler = null;

    @JsonProperty
    protected int seaLevel = -1;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        this.ground = this.ground == null ? generator.ground() : this.ground;
        Preconditions.checkState(this.top != null, "top must be set!");
        Preconditions.checkState(this.filler != null, "filler must be set!");

        this.seaLevel = this.seaLevel < 0 ? generator.seaLevel() : this.seaLevel;
    }

    @Override
    public void decorate(PRandom random, Chunk chunk, int x, int z) {
        boolean placed = false;
        final int depth = this.getDepthNoise(chunk, random, x, z);

        final int max = this.height == null ? 255 : this.height.max;
        final int min = this.height == null ? 0 : this.height.min;

        for (int y = chunk.getHighestBlock(x, z); y >= min; y--) {
            if (chunk.getBlock(x, y, z, 0) == this.ground) {
                if (!placed) {
                    placed = true;
                    if (y <= max) {
                        if (y + 1 > this.seaLevel) {
                            if (y < 255 && this.cover != null) {
                                chunk.setBlock(x, y + 1, z, 0, this.cover);
                            }
                            chunk.setBlock(x, y--, z, 0, this.top);
                        }
                        for (int i = depth - 1; i >= 0 && y >= 0; i--, y--) {
                            if (chunk.getBlock(x, y, z, 0) == this.ground) {
                                chunk.setBlock(x, y, z, 0, this.filler);
                            } else {
                                //we hit air prematurely, abort!
                                placed = false;
                                break;
                            }
                        }
                    }
                }
            } else {
                //reset when we hit air again
                placed = false;
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
