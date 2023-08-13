package org.cloudburstmc.server.level.generator.standard.generation.noise;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.OpenSimplexNoiseEngine;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.util.Identifier;

import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class OpenSimplexDefaultNoiseGenerator extends DefaultNoiseGenerator {
    public static final Identifier ID = Identifier.parse("cloudburst:opensimplex");

    @Override
    protected NoiseSource create0(@NonNull RandomGenerator random) {
        return new OpenSimplexNoiseEngine((PRandom) random);
    }
}
