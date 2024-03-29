package org.cloudburstmc.server.level.generator.standard.generation.decorator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.generation.noise.NoiseGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;

import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Similar to {@link SurfaceDecorator}, but switches between two different decorators based on the elevation of the highest block in the chunk.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class HeightSelectionDecorator extends AbstractGenerationPass implements Decorator {
    public static final Identifier ID = Identifier.parse("cloudburst:height");

    protected NoiseSource threshold;

    @JsonProperty
    protected double additionalOffset = 0.0d;

    @JsonProperty
    protected Decorator[] below = Decorator.EMPTY_ARRAY;

    @JsonProperty
    protected Decorator[] above = Decorator.EMPTY_ARRAY;

    @JsonProperty("threshold")
    protected NoiseGenerator thresholdNoise;

    @JsonProperty
    protected boolean addSeaLevelToOffset = false;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        RandomGenerator random = new FastPRandom(localSeed);

        this.threshold = Objects.requireNonNull(this.thresholdNoise, "threshold must be set!").create(random);
        this.thresholdNoise = null;

        for (Decorator decorator : Objects.requireNonNull(this.below, "below must be set!")) {
            decorator.init(levelSeed, random.nextLong(), generator);
        }
        for (Decorator decorator : Objects.requireNonNull(this.above, "above must be set!")) {
            decorator.init(levelSeed, random.nextLong(), generator);
        }

        if (this.addSeaLevelToOffset) {
            this.additionalOffset += generator.seaLevel();
        }
    }

    @Override
    public void decorate(RandomGenerator random, Chunk chunk, int x, int z) {
        int height = chunk.getHighestBlock(x, z);
        double noise = this.threshold.get((chunk.getX() << 4) + x, (chunk.getZ() << 4) + z) + this.additionalOffset;
        for (Decorator decorator : height < noise ? this.below : this.above) {
            decorator.decorate(random, chunk, x, z);
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
