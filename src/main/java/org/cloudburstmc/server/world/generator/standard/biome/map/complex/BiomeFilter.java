package org.cloudburstmc.server.world.generator.standard.biome.map.complex;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.world.generator.standard.biome.GenerationBiome;
import org.cloudburstmc.server.world.generator.standard.misc.IntArrayAllocator;

import java.util.Collection;

/**
 * A single filter for {@link ComplexBiomeMap}.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize(using = BiomeFilterDeserializer.class)
public interface BiomeFilter {
    void init(long seed, PRandom random);

    Collection<GenerationBiome> getAllBiomes();

    int[] get(int x, int z, int sizeX, int sizeZ, IntArrayAllocator alloc);
}
