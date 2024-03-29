package org.cloudburstmc.server.level.generator.standard.biome.map.complex;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.daporkchop.lib.common.util.PValidation;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.biome.GenerationBiome;
import org.cloudburstmc.server.level.generator.standard.biome.map.BiomeMap;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;
import org.cloudburstmc.server.level.generator.standard.misc.IntArrayAllocator;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link BiomeMap} which uses a series of iterative passes ("filters") to progressively select a biome.
 * <p>
 * This follows a similar pattern to vanilla biome selection.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class ComplexBiomeMap extends AbstractGenerationPass implements BiomeMap {
    public static final Identifier ID = Identifier.parse("cloudburst:complex");

    @JsonProperty
    protected BiomeFilter root;

    @JsonProperty
    protected GenerationBiome fallback;

    protected final Int2ObjectMap<GenerationBiome> internalIdLookup = new Int2ObjectOpenHashMap<>();

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        Objects.requireNonNull(this.root, "root must be set!");
        Objects.requireNonNull(this.fallback, "fallback must be set!");

        this.root.init(localSeed, new FastPRandom(localSeed));

        this.possibleBiomes().forEach(biome -> this.internalIdLookup.put(biome.getInternalId(), biome));
    }

    @Override
    public GenerationBiome get(int x, int z) {
        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
        int[] arr = this.root.get(x, z, 1, 1, alloc);

        GenerationBiome biome = this.internalIdLookup.getOrDefault(arr[0], this.fallback);
        alloc.release(arr);
        return biome;
    }

    @Override
    public GenerationBiome[] getRegion(GenerationBiome[] arr, int x, int z, int sizeX, int sizeZ) {
        int totalSize = PValidation.positive(sizeX) * PValidation.positive(sizeZ);
        if (arr == null || arr.length < totalSize) {
            arr = new GenerationBiome[totalSize];
        }

        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
        int[] biomes = this.root.get(x, z, sizeX, sizeZ, alloc);
        for (int i = 0; i < totalSize; i++) {
            arr[i] = this.internalIdLookup.getOrDefault(biomes[i], this.fallback);
        }
        alloc.release(biomes);
        return arr;
    }

    @Override
    public Identifier[] getRegionIds(Identifier[] arr, int x, int z, int sizeX, int sizeZ) {
        int totalSize = PValidation.positive(sizeX) * PValidation.positive(sizeZ);
        if (arr == null || arr.length < totalSize) {
            arr = new Identifier[totalSize];
        }

        IntArrayAllocator alloc = IntArrayAllocator.DEFAULT.get();
        int[] biomes = this.root.get(x, z, sizeX, sizeZ, alloc);
        for (int i = 0; i < totalSize; i++) {
            arr[i] = this.internalIdLookup.getOrDefault(biomes[i], this.fallback).getId();
        }
        alloc.release(biomes);
        return arr;
    }

    @Override
    public boolean needsCaching() {
        return true;
    }

    @Override
    public Set<GenerationBiome> possibleBiomes() {
        return new LinkedHashSet<>(this.root.getAllBiomes());
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
