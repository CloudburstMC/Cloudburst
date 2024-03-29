package org.cloudburstmc.server.level.generator.standard.biome.map.complex;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.Objects;

import static net.daporkchop.lib.common.math.PMath.mix32;
import static net.daporkchop.lib.common.math.PMath.mix64;

/**
 * Base implementation of a {@link BiomeFilter}.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public abstract class AbstractBiomeFilter implements BiomeFilter {
    protected long seed;

    @Override
    public void init(long seed, PRandom random) {
        this.seed = random.nextLong();
    }

    protected int random(int x, int z, int i, int bound) {
        return (mix32(mix64(this.seed + i * 0x9e3779b97f4a7c15L) ^ mix64(CloudChunk.key(x, z))) >>> 1) % bound;
    }

    @JsonDeserialize
    public abstract static class Next extends AbstractBiomeFilter {
        @JsonProperty
        @JsonAlias({"parent"})
        protected BiomeFilter next;

        @Override
        public void init(long seed, PRandom random) {
            Objects.requireNonNull(this.next, "next must be set!").init(seed, random);

            super.init(seed, random);
        }
    }
}
