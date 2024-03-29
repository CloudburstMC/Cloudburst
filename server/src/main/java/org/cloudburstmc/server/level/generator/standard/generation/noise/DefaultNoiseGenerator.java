package org.cloudburstmc.server.level.generator.standard.generation.noise;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.filter.ScaleOctavesOffsetFilter;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.level.generator.standard.misc.DoubleTriple;

import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public abstract class DefaultNoiseGenerator implements NoiseGenerator {
    @JsonProperty
    protected DoubleTriple scale = DoubleTriple.ONE;

    @JsonProperty
    protected int octaves = 1;

    @JsonProperty
    protected double factor = 1.0d;

    @JsonProperty
    protected double offset = 0.0d;

    @Override
    public NoiseSource create(@NonNull RandomGenerator random) {
        Preconditions.checkArgument(this.octaves >= 1, "octaves (%d) must be at least 1!", this.octaves);
        return new ScaleOctavesOffsetFilter(this.create0(random), this.scale.getX(), this.scale.getY(), this.scale.getZ(), this.octaves, this.factor, this.offset);
    }

    protected abstract NoiseSource create0(@NonNull RandomGenerator random);
}
