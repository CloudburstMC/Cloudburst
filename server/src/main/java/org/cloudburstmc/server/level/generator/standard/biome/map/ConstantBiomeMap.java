package org.cloudburstmc.server.level.generator.standard.biome.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.common.util.PValidation;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.biome.GenerationBiome;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link BiomeMap} which returns a constant biome.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class ConstantBiomeMap extends AbstractGenerationPass implements BiomeMap {
    public static final Identifier ID = Identifier.parse("cloudburst:constant");

    @JsonProperty
    private GenerationBiome biome;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        Objects.requireNonNull(this.biome, "biome must be set!");
    }

    @Override
    public GenerationBiome get(int x, int z) {
        return this.biome;
    }

    @Override
    public GenerationBiome[] getRegion(GenerationBiome[] arr, int x, int z, int sizeX, int sizeZ) {
        int totalSize = PValidation.positive(sizeX) * PValidation.positive(sizeZ);
        if (arr == null || arr.length < totalSize) {
            arr = new GenerationBiome[totalSize];
        }

        Arrays.fill(arr, 0, totalSize, this.biome);
        return arr;
    }

    @Override
    public Identifier[] getRegionIds(Identifier[] arr, int x, int z, int sizeX, int sizeZ) {
        int totalSize = PValidation.positive(sizeX) * PValidation.positive(sizeZ);
        if (arr == null || arr.length < totalSize) {
            arr = new Identifier[totalSize];
        }

        Arrays.fill(arr, 0, totalSize, this.biome.getId());
        return arr;
    }

    @Override
    public boolean needsCaching() {
        //obviously this doesn't need to be cached, there's no logic involved at all :P
        return false;
    }

    @Override
    public Set<GenerationBiome> possibleBiomes() {
        return Collections.singleton(this.biome);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
