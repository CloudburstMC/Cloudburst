package org.cloudburstmc.server.level.generator.standard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.ChunkManager;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.generator.Generator;
import org.cloudburstmc.server.level.generator.GeneratorFactory;
import org.cloudburstmc.server.level.generator.standard.biome.GenerationBiome;
import org.cloudburstmc.server.level.generator.standard.biome.map.BiomeMap;
import org.cloudburstmc.server.level.generator.standard.biome.map.BiomeMapReferenceDeserializer;
import org.cloudburstmc.server.level.generator.standard.biome.map.CachingBiomeMap;
import org.cloudburstmc.server.level.generator.standard.finish.Finisher;
import org.cloudburstmc.server.level.generator.standard.generation.decorator.Decorator;
import org.cloudburstmc.server.level.generator.standard.generation.density.DensitySource;
import org.cloudburstmc.server.level.generator.standard.generation.density.DensitySourceReferenceDeserializer;
import org.cloudburstmc.server.level.generator.standard.misc.GenerationPass;
import org.cloudburstmc.server.level.generator.standard.misc.NextGenerationPass;
import org.cloudburstmc.server.level.generator.standard.population.Populator;
import org.cloudburstmc.server.level.generator.standard.store.StandardGeneratorStores;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.fallbackIfNull;

/**
 * Main class of the Cloudburst Standard Generator.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor
@Accessors(fluent = true)
public final class StandardGenerator implements Generator {
    public static final Identifier ID = Identifier.parse("cloudburst:standard");

    private static final String DEFAULT_PRESET = "minecraft:overworld";

    public static final GeneratorFactory FACTORY = (seed, options) -> {
        Identifier presetId = Identifier.parse(Strings.isNullOrEmpty(options) ? DEFAULT_PRESET : options);
        try (InputStream in = StandardGeneratorUtils.read("preset", presetId)) {
            synchronized (StandardGenerator.class) {
                return Bootstrap.YAML_MAPPER.readValue(in, StandardGenerator.class).init(seed);
            }
        } catch (IOException e) {
            throw new RuntimeException("While decoding preset " + presetId, e);
        }
    };

    public static final int STEP_X = 4;
    public static final int STEP_Y = 8;
    public static final int STEP_Z = STEP_X;

    public static final int SAMPLES_X = 16 / STEP_X;
    public static final int SAMPLES_Y = 256 / STEP_Y;
    public static final int SAMPLES_Z = 16 / STEP_Z;

    public static final int CACHE_X = SAMPLES_X + 1;
    public static final int CACHE_Y = SAMPLES_Y + 1;
    public static final int CACHE_Z = SAMPLES_Z + 1;

    public static final double SCALE_X = 1.0d / STEP_X;
    public static final double SCALE_Y = 1.0d / STEP_Y;
    public static final double SCALE_Z = 1.0d / STEP_Z;

    private static final Ref<ThreadData> THREAD_DATA_CACHE = ThreadRef.soft(ThreadData::new);

    @JsonProperty
    @JsonDeserialize(using = BiomeMapReferenceDeserializer.class)
    private BiomeMap biomes;
    @JsonProperty
    @JsonDeserialize(using = DensitySourceReferenceDeserializer.class)
    private DensitySource density;
    @JsonProperty
    private Decorator[] decorators = Decorator.EMPTY_ARRAY;
    @JsonProperty
    private Populator[] populators = Populator.EMPTY_ARRAY;
    @JsonProperty
    private Finisher[] finishers = Finisher.EMPTY_ARRAY;

    private final Int2ObjectMap<Decorator[]> decoratorLookup = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Populator[]> populatorLookup = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Finisher[]> finisherLookup = new Int2ObjectOpenHashMap<>();

    @JsonProperty("groundBlock")
    @Getter
    private BlockState ground = null;
    @JsonProperty("seaBlock")
    @Getter
    private BlockState sea = null;
    @JsonProperty
    @Getter
    private int seaLevel = -1;

    private StandardGenerator init(long seed) {
        try {
            Collection<GenerationBiome> biomes = this.biomes.possibleBiomes();

            Preconditions.checkState(this.ground != null, "groundBlock must be set!");
            Preconditions.checkState(this.seaLevel < 0 || this.sea != null, "seaBlock and seaLevel must either both be set or be omitted!");

            Collection<GenerationPass> generationPasses = new LinkedHashSet<>(); //preserve order but don't allow duplicates
            generationPasses.add(Objects.requireNonNull(this.density, "density must be set!"));
            generationPasses.add(Objects.requireNonNull(this.biomes, "biomes must be set!"));
            Collections.addAll(generationPasses, this.decorators = fallbackIfNull(this.decorators, Decorator.EMPTY_ARRAY));
            Collections.addAll(generationPasses, this.populators = fallbackIfNull(this.populators, Populator.EMPTY_ARRAY));
            Collections.addAll(generationPasses, this.finishers = fallbackIfNull(this.finishers, Finisher.EMPTY_ARRAY));

            for (GenerationBiome biome : biomes) {
                Collections.addAll(generationPasses, biome.getDecorators());
                Collections.addAll(generationPasses, biome.getPopulators());
                Collections.addAll(generationPasses, biome.getFinishers());

                this.decoratorLookup.put(biome.getInternalId(), Arrays.stream(this.decorators)
                        .flatMap(decorator -> decorator instanceof NextGenerationPass ? Arrays.stream(biome.getDecorators()) : Stream.of(decorator))
                        .toArray(Decorator[]::new));
                this.populatorLookup.put(biome.getInternalId(), Arrays.stream(this.populators)
                        .flatMap(populator -> populator instanceof NextGenerationPass ? Arrays.stream(biome.getPopulators()) : Stream.of(populator))
                        .toArray(Populator[]::new));
                this.finisherLookup.put(biome.getInternalId(), Arrays.stream(this.finishers)
                        .flatMap(finisher -> finisher instanceof NextGenerationPass ? Arrays.stream(biome.getFinishers()) : Stream.of(finisher))
                        .toArray(Finisher[]::new));
            }

            RandomGenerator random = new FastPRandom(seed);
            for (GenerationPass pass : generationPasses) {
                pass.init(seed, random.nextLong(), this);
            }

            if (this.biomes.needsCaching()) {
                this.biomes = new CachingBiomeMap(this.biomes);
            }
            return this;
        } finally {
            //reset generation biome store to ensure that the replacers/decorators/populators for a given islandBiomes aren't initialized multiple times for multiple worlds
            StandardGeneratorStores.generationBiome().reset();
        }
    }

    @Override
    public void generate(RandomGenerator random, Chunk chunk, int chunkX, int chunkZ) {
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;
        final ThreadData threadData = THREAD_DATA_CACHE.get();

        //compute initial densities
        final double[] densityCache = threadData.densityCache
                = this.density.get(threadData.densityCache, 0, this.biomes, baseX, 0, baseZ, CACHE_X, CACHE_Y, CACHE_Z, STEP_X, STEP_Y, STEP_Z);

        //interpolate densities and build initial surfaces
        for (int i = 0, sectionX = 0; sectionX < SAMPLES_X; sectionX++) {
            for (int sectionZ = 0; sectionZ < SAMPLES_Z; sectionZ++) {
                for (int sectionY = 0; sectionY < SAMPLES_Y; sectionY++, i++) {
                    double dxyz = densityCache[i];
                    double dxYz = densityCache[i + 1];
                    double dxyZ = densityCache[i + CACHE_Y];
                    double dxYZ = densityCache[i + CACHE_Y + 1];
                    double dXyz = densityCache[i + CACHE_Y * CACHE_Z];
                    double dXYz = densityCache[i + CACHE_Y * CACHE_Z + 1];
                    double dXyZ = densityCache[i + CACHE_Y * CACHE_Z + CACHE_Y];
                    double dXYZ = densityCache[i + CACHE_Y * CACHE_Z + CACHE_Y + 1];

                    double bx00 = dxyz;
                    double bx01 = dxyZ;
                    double bx10 = dxYz;
                    double bx11 = dxYZ;
                    double gx00 = (dXyz - dxyz) * SCALE_X;
                    double gx01 = (dXyZ - dxyZ) * SCALE_X;
                    double gx10 = (dXYz - dxYz) * SCALE_X;
                    double gx11 = (dXYZ - dxYZ) * SCALE_X;

                    for (int stepX = 0; stepX < STEP_X; stepX++) {
                        double ix00 = bx00 + gx00 * stepX;
                        double ix01 = bx01 + gx01 * stepX;
                        double ix10 = bx10 + gx10 * stepX;
                        double ix11 = bx11 + gx11 * stepX;

                        double by0 = ix00;
                        double by1 = ix01;
                        double gy0 = (ix10 - ix00) * SCALE_Y;
                        double gy1 = (ix11 - ix01) * SCALE_Y;

                        for (int stepY = 0; stepY < STEP_Y; stepY++) {
                            double iy0 = by0 + gy0 * stepY;
                            double iy1 = by1 + gy1 * stepY;

                            double bz = iy0;
                            double gz = (iy1 - iy0) * SCALE_Z;

                            for (int stepZ = 0; stepZ < STEP_Z; stepZ++) {
                                double iz = bz + gz * stepZ;

                                int blockX = sectionX * STEP_X | stepX;
                                int blockY = sectionY * STEP_Y | stepY;
                                int blockZ = sectionZ * STEP_Z | stepZ;

                                if (iz > 0.0d) {
                                    chunk.setBlock(blockX, blockY, blockZ, 0, this.ground);
                                } else if (blockY <= this.seaLevel) {
                                    chunk.setBlock(blockX, blockY, blockZ, 0, this.sea);
                                }
                            }
                        }
                    }
                }
                i++;
            }
            i += CACHE_Y;
        }

        //run decorators and set biomes
        GenerationBiome[] biomes = threadData.biomes = this.biomes.getRegion(threadData.biomes, baseX, baseZ, 16, 16);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                GenerationBiome biome = biomes[(x << 4) | z];
                chunk.setBiome(x, z, biome.getRuntimeId());
                for (Decorator decorator : this.decoratorLookup.get(biome.getInternalId())) {
                    decorator.decorate(random, chunk, x, z);
                }
            }
        }

        //clean up
        Arrays.fill(biomes, null);
    }

    @Override
    public void populate(RandomGenerator random, ChunkManager level, int chunkX, int chunkZ) {
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;
        final ThreadData threadData = THREAD_DATA_CACHE.get();

        //run populators
        GenerationBiome[] biomes = threadData.biomes = this.biomes.getRegion(threadData.biomes, baseX, baseZ, 16, 16);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (Populator populator : this.populatorLookup.get(biomes[(x << 4) | z].getInternalId())) {
                    populator.populate(random, level, baseX + x, baseZ + z);
                }
            }
        }

        //clean up
        Arrays.fill(biomes, null);
    }

    @Override
    public void finish(RandomGenerator random, ChunkManager level, int chunkX, int chunkZ) {
        final int baseX = chunkX << 4;
        final int baseZ = chunkZ << 4;
        final ThreadData threadData = THREAD_DATA_CACHE.get();

        //run finishers
        GenerationBiome[] biomes = threadData.biomes = this.biomes.getRegion(threadData.biomes, baseX, baseZ, 16, 16);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (Finisher finisher : this.finisherLookup.get(biomes[(x << 4) | z].getInternalId())) {
                    finisher.finish(random, level, baseX + x, baseZ + z);
                }
            }
        }

        //clean up
        Arrays.fill(biomes, null);
    }

    @JsonSetter("copyFrom")
    private void copyFrom(@NonNull Identifier presetId) {
        try (InputStream in = StandardGeneratorUtils.read("preset", presetId)) {
            StandardGenerator from = Bootstrap.YAML_MAPPER.readValue(in, StandardGenerator.class);
            this.biomes = from.biomes;
            this.density = from.density;
            this.decorators = from.decorators;
            this.populators = from.populators;
            this.finishers = from.finishers;
            this.ground = from.ground;
            this.sea = from.sea;
            this.seaLevel = from.seaLevel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class ThreadData {
        private double[] densityCache;
        private GenerationBiome[] biomes;
    }
}
