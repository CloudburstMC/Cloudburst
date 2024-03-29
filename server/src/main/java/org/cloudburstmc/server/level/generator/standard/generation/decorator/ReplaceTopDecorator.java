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
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Replaces the top block in a chunk with a given replacement if it matches a given filter.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class ReplaceTopDecorator implements Decorator {
    public static final Identifier ID = Identifier.parse("cloudburst:replace_top");

    @JsonProperty
    protected BlockFilter replace;

    @JsonProperty
    protected IntRange height = IntRange.WHOLE_WORLD;

    @JsonProperty
    protected BlockState block = null;

    @Override
    public void init(long levelSeed, long localSeed, StandardGenerator generator) {
        Objects.requireNonNull(this.replace, "replace must be set!");
        Objects.requireNonNull(this.height, "height must be set!");
        Preconditions.checkState(this.block != null, "block must be set!");
    }

    @Override
    public void decorate(RandomGenerator random, Chunk chunk, int x, int z) {
        int y = chunk.getHighestBlock(x, z);
        if (y >= 0 && this.replace.test(chunk.getBlock(x, y, z, 0)) && this.height.contains(y)) {
            chunk.setBlock(x, y, z, 0, this.block);
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
