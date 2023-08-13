package org.cloudburstmc.server.level.generator.standard.generation.noise;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.SimplexNoiseEngine;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.util.Identifier;

import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class SimplexDefaultNoiseGenerator extends DefaultNoiseGenerator {
    public static final Identifier ID = Identifier.parse("cloudburst:simplex");

    @Override
    protected NoiseSource create0(@NonNull RandomGenerator random) {
        return new SimplexNoiseEngine((PRandom) random);
    }
}
