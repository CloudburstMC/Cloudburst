package org.cloudburstmc.server.level.generator.standard.generation.density;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.biome.map.BiomeMap;
import org.cloudburstmc.server.level.generator.standard.generation.noise.NoiseGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;

import static java.lang.Math.cos;
import static java.util.Objects.requireNonNull;
import static net.daporkchop.lib.common.math.PMath.clamp;
import static net.daporkchop.lib.common.math.PMath.lerp;

/**
 * A {@link NoiseSource} that provides noise similar to that of vanilla's nether terrain.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class NetherDensitySource extends AbstractGenerationPass implements DensitySource {
    public static final Identifier ID = Identifier.parse("cloudburst:nether");

    private static final double NOISE_SCALE_FACTOR = ((1 << 16) - 1.0d) / 512.0d;

    //these fields aren't sorted in ascending order by size (so there's a possibility that fields might not be word-aligned), however they ARE sorted
    // by the order in which they're used (so they can be prefetched into the cache)
    private NoiseSource selector;
    private NoiseSource low;
    private NoiseSource high;

    @JsonProperty
    private NoiseGenerator selectorNoise;
    @JsonProperty
    private NoiseGenerator lowNoise;
    @JsonProperty
    private NoiseGenerator highNoise;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        PRandom random = new FastPRandom(localSeed);
        this.selector = requireNonNull(this.selectorNoise, "selectorNoise must be set!").create(new FastPRandom(random.nextLong()));
        this.low = requireNonNull(this.lowNoise, "lowNoise must be set!").create(new FastPRandom(random.nextLong()));
        this.high = requireNonNull(this.highNoise, "highNoise must be set!").create(new FastPRandom(random.nextLong()));
        this.selectorNoise = this.lowNoise = this.highNoise = null;
    }

    @Override
    public double get(@NonNull BiomeMap biomes, int x, int y, int z) {
        if (y >= 128) {
            return 0.0d;
        }

        double selector = clamp(this.selector.get(x, y, (double) z), 0.0d, 1.0d);
        double low = this.low.get(x, y, (double) z) * NOISE_SCALE_FACTOR;
        double high = this.high.get(x, y, (double) z) * NOISE_SCALE_FACTOR;

        double outputNoise = lerp(low, high, selector);

        double threshold = y * 0.125d;
        double offset = cos(threshold * Math.PI * 6.0d / 17.0d) * 2.0d;

        if (threshold > 8.0d) {
            threshold = 16.0d - threshold;
        }
        if (threshold < 4.0d) {
            threshold = 4.0d - threshold;
            offset -= threshold * threshold * threshold * 10.0d;
        }

        return outputNoise - offset;
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
