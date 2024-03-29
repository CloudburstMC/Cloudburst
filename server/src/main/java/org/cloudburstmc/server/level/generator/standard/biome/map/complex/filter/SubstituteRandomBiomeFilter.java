package org.cloudburstmc.server.level.generator.standard.biome.map.complex.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.biome.GenerationBiome;
import org.cloudburstmc.server.level.generator.standard.biome.map.complex.AbstractBiomeFilter;
import org.cloudburstmc.server.level.generator.standard.misc.IntArrayAllocator;
import org.cloudburstmc.server.level.generator.standard.store.StandardGeneratorStores;

import java.util.*;

/**
 * Replaces specific biome with a replacement islandBiomes randomly selected from a list.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class SubstituteRandomBiomeFilter extends AbstractBiomeFilter.Next {
    public static final Identifier ID = Identifier.parse("cloudburst:substitute_random");

    protected final Int2ObjectMap<int[]> replacements = new Int2ObjectOpenHashMap<>();

    @JsonProperty
    protected int chance = 1;

    @JsonProperty
    protected Map<String, GenerationBiome[]> biomes;

    @Override
    public void init(long seed, PRandom random) {
        Objects.requireNonNull(this.biomes, "biomes must be set!").forEach((key, arr) -> {
            int replaceId = StandardGeneratorStores.generationBiome().find(Identifier.parse(key)).getInternalId();
            int[] replacementIds = Arrays.stream(arr).mapToInt(GenerationBiome::getInternalId).toArray();
            this.replacements.put(replaceId, replacementIds);
        });

        super.init(seed, random);
    }

    @Override
    public Collection<GenerationBiome> getAllBiomes() {
        Collection<GenerationBiome> biomes = new ArrayList<>(this.next.getAllBiomes());
        this.biomes.values().forEach(arr -> Collections.addAll(biomes, arr));
        return biomes;
    }

    @Override
    public int[] get(int x, int z, int sizeX, int sizeZ, IntArrayAllocator alloc) {
        int[] out = this.next.get(x, z, sizeX, sizeZ, alloc);

        final int chance = this.chance;

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                if (chance > 1 && this.random(x + dx, z + dz, 0, chance) != 0) {
                    continue;
                }
                int i = dx * sizeZ + dz;

                int[] replacementIds = this.replacements.get(out[i]);
                if (replacementIds != null) {
                    out[i] = replacementIds[this.random(x + dx, z + dz, 1, replacementIds.length)];
                }
            }
        }

        return out;
    }
}
