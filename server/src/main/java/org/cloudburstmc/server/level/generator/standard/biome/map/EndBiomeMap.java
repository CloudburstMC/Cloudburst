package org.cloudburstmc.server.level.generator.standard.biome.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.daporkchop.lib.common.util.PValidation;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.biome.GenerationBiome;
import org.cloudburstmc.server.level.generator.standard.generation.density.EndDensitySource;
import org.cloudburstmc.server.level.generator.standard.misc.AbstractGenerationPass;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import java.util.Set;

@JsonDeserialize
public class EndBiomeMap extends AbstractGenerationPass implements BiomeMap {

    public static final Identifier ID = Identifier.parse("cloudburst:end");

    private static final long CENTRAL_ISLAND_RADIUS_SQUARED = 4096;
    private static final double HIGHLANDS_THRESHOLD = 0.25;
    private static final double MIDLANDS_THRESHOLD = -0.0625;
    private static final double SMALL_ISLANDS_THRESHOLD = -0.21875;

    @JsonProperty
    private GenerationBiome end;
    @JsonProperty
    private GenerationBiome highlands;
    @JsonProperty
    private GenerationBiome midlands;
    @JsonProperty
    private GenerationBiome smallIslands;
    @JsonProperty
    private GenerationBiome barrens;

    private EndDensitySource density;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        this.possibleBiomes();
        if (!(generator.density() instanceof EndDensitySource endDensity)) {
            throw new IllegalStateException("End biome map requires the End density source");
        }
        this.density = endDensity;
    }

    @Override
    public GenerationBiome get(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= CENTRAL_ISLAND_RADIUS_SQUARED) {
            return this.end;
        }

        int sampleX = (chunkX * 2 + 1) * 8;
        int sampleZ = (chunkZ * 2 + 1) * 8;
        double erosion = this.density.getIslandErosion(sampleX, sampleZ);
        if (erosion > HIGHLANDS_THRESHOLD) {
            return this.highlands;
        }

        if (erosion >= MIDLANDS_THRESHOLD) {
            return this.midlands;
        }

        return erosion < SMALL_ISLANDS_THRESHOLD ? this.smallIslands : this.barrens;
    }

    @Override
    public GenerationBiome[] getRegion(GenerationBiome[] biomes, int x, int z, int sizeX, int sizeZ) {
        int size = PValidation.positive(sizeX) * PValidation.positive(sizeZ);
        if (biomes == null || biomes.length < size) {
            biomes = new GenerationBiome[size];
        }

        for (int offsetX = 0; offsetX < sizeX; offsetX++) {
            for (int offsetZ = 0; offsetZ < sizeZ; offsetZ++) {
                biomes[offsetX * sizeZ + offsetZ] = get(x + offsetX, z + offsetZ);
            }
        }

        return biomes;
    }

    @Override
    public Identifier[] getRegionIds(Identifier[] identifiers, int x, int z, int sizeX, int sizeZ) {
        int size = PValidation.positive(sizeX) * PValidation.positive(sizeZ);
        if (identifiers == null || identifiers.length < size) {
            identifiers = new Identifier[size];
        }

        for (int offsetX = 0; offsetX < sizeX; offsetX++) {
            for (int offsetZ = 0; offsetZ < sizeZ; offsetZ++) {
                identifiers[offsetX * sizeZ + offsetZ] = get(x + offsetX, z + offsetZ).getId();
            }
        }

        return identifiers;
    }

    @Override
    public boolean needsCaching() {
        return true;
    }

    @Override
    public Set<GenerationBiome> possibleBiomes() {
        return Set.of(
                Objects.requireNonNull(this.end, "end must be set"),
                Objects.requireNonNull(this.highlands, "highlands must be set"),
                Objects.requireNonNull(this.midlands, "midlands must be set"),
                Objects.requireNonNull(this.smallIslands, "smallIslands must be set"),
                Objects.requireNonNull(this.barrens, "barrens must be set")
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
