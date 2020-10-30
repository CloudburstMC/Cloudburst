package org.cloudburstmc.server.level.generator.standard.generation.decorator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.generation.noise.NoiseGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Arrays;
import java.util.Objects;

import static net.daporkchop.lib.common.math.PMath.roundI;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public class MesaSurfaceDecorator extends DepthNoiseDecorator {
    public static final Identifier ID = Identifier.fromString("cloudburst:mesa_surface");

    protected static final int BAND_COUNT = 64;
    protected static final int BAND_MASK = BAND_COUNT - 1;

    @JsonProperty
    protected BlockState ground = null;

    @JsonProperty
    protected int seaLevel = -1;

    protected NoiseSource bandOffset;

    protected BlockState[] bands = new BlockState[BAND_COUNT];

    @JsonProperty
    protected ConstantBlock base;

    @JsonProperty("bands")
    protected Band[] layers;

    @JsonProperty("bandOffset")
    protected NoiseGenerator bandOffsetNoise;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        this.ground = this.ground == null ? generator.ground() : this.ground;
        this.seaLevel = this.seaLevel < 0 ? generator.seaLevel() : this.seaLevel;

        PRandom random = new FastPRandom(localSeed);
        Arrays.fill(this.bands, Objects.requireNonNull(this.base, "base must be set!").state());

        for (Band band : Objects.requireNonNull(this.layers, "bands must be set!")) {
            band.apply(random, this.bands);
        }
        this.layers = null;

        this.bandOffset = Objects.requireNonNull(this.bandOffsetNoise, "bandOffset must be set!").create(random);
        this.bandOffsetNoise = null;
    }

    @Override
    public void decorate(PRandom random, IChunk chunk, int x, int z) {
        final int blockX = (chunk.getX() << 4) + x;
        final int blockZ = (chunk.getZ() << 4) + z;

        final BlockState ground = this.ground;
        final int minHeight = this.seaLevel + this.getDepthNoise(random, blockX, blockZ);

        for (int y = chunk.getHighestBlock(x, z); y >= minHeight; y--) {
            if (chunk.getBlock(x, y, z, 0) == ground) {
                chunk.setBlock(x, y, z, 0, this.getBand(blockX, y, blockZ));
            }
        }
    }

    protected BlockState getBand(int x, int y, int z) {
        return this.bands[(roundI(this.bandOffset.get(x, z)) + y) & BAND_MASK];
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @JsonDeserialize
    protected static class Band {
        @JsonProperty
        protected IntRange count;

        @JsonProperty
        protected IntRange size = IntRange.ONE;

        @JsonProperty
        protected ConstantBlock block;

        @JsonProperty
        protected ConstantBlock above;

        @JsonProperty
        protected ConstantBlock below;

        public void apply(@NonNull PRandom random, @NonNull BlockState[] bands) {
            Objects.requireNonNull(this.count, "count must be set!");
            Objects.requireNonNull(this.size, "size must be set!");
            Objects.requireNonNull(this.block, "block must be set!");

            BlockState state = this.block.state();
            for (int n = this.count.rand(random) - 1; n >= 0; n--) {
                int y = random.nextInt(BAND_COUNT);
                for (int i = 0, size = this.size.rand(random); i < size && y + i < BAND_COUNT; i++) {
                    bands[y + i] = state;

                    if (this.below != null && y + i > 0 && random.nextBoolean()) {
                        bands[y + i - 1] = this.below.state();
                    }
                    if (this.above != null && y + i < BAND_COUNT - 1 && random.nextBoolean()) {
                        bands[y + i + 1] = this.above.state();
                    }
                }
            }
        }
    }
}
