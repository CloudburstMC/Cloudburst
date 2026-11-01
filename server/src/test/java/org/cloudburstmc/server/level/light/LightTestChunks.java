package org.cloudburstmc.server.level.light;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.level.generator.Generator;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.manager.PopulationChunkManager;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.TestLevels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Generated overworld chunks for light tests.
 */
@UtilityClass
public class LightTestChunks {

    /**
     * Generates a square of overworld chunks and populates its inner square.
     *
     * @param seed         the level seed
     * @param originChunkX the inner square's lowest chunk x
     * @param originChunkZ the inner square's lowest chunk z
     * @param width        the inner square's width in chunks
     * @return the populated chunks keyed by {@code CloudChunk.key}
     */
    public static Long2ObjectMap<CloudChunk> overworld(long seed, int originChunkX, int originChunkZ, int width) {
        BlockTestRegistry.get();
        Generator generator = StandardGenerator.FACTORY.create(seed, "minecraft:overworld");
        Level level = TestLevels.level(DimensionHeight.OVERWORLD.getMinHeight(), DimensionHeight.OVERWORLD.getMaxHeight(), Map.of("getSeed", seed));

        Long2ObjectMap<CloudChunk> generated = new Long2ObjectOpenHashMap<>();
        for (int chunkX = originChunkX - 1; chunkX < originChunkX + width + 1; chunkX++) {
            for (int chunkZ = originChunkZ - 1; chunkZ < originChunkZ + width + 1; chunkZ++) {
                CloudChunk chunk = new CloudChunk(chunkX, chunkZ, level);
                RandomGenerator random = new FastPRandom(chunkX * 3053330778986901431L ^ chunkZ * 1517227374085824433L ^ seed);
                try (LockedChunk locked = chunk.lockForWrite()) {
                    generator.generate(random, locked, chunkX, chunkZ);
                    chunk.advanceGenerationStatus(ChunkGenerationStatus.GENERATED);
                }
                generated.put(CloudChunk.key(chunkX, chunkZ), chunk);
            }
        }

        Long2ObjectMap<CloudChunk> populated = new Long2ObjectOpenHashMap<>();
        for (int chunkX = originChunkX; chunkX < originChunkX + width; chunkX++) {
            for (int chunkZ = originChunkZ; chunkZ < originChunkZ + width; chunkZ++) {
                CloudChunk chunk = generated.get(CloudChunk.key(chunkX, chunkZ));
                List<CloudChunk> surrounding = new ArrayList<>(9);
                for (int neighborX = chunkX - 1; neighborX <= chunkX + 1; neighborX++) {
                    for (int neighborZ = chunkZ - 1; neighborZ <= chunkZ + 1; neighborZ++) {
                        CloudChunk neighbor = generated.get(CloudChunk.key(neighborX, neighborZ));
                        Preconditions.checkState(neighbor != null && neighbor.isGenerated(),
                                "Chunk %s,%s was used for population before being generated!", neighborX, neighborZ);
                        surrounding.add(neighbor);
                    }
                }

                RandomGenerator random = new FastPRandom(chunkX * 6169336838570288771L ^ chunkZ * 1173358236373774883L ^ seed);
                LockedChunk[] lockedChunks = surrounding.stream()
                        .sorted()
                        .map(CloudChunk::lockForWrite)
                        .toArray(LockedChunk[]::new);
                try {
                    generator.populate(random, new PopulationChunkManager(chunk, lockedChunks, seed), chunkX, chunkZ);
                    chunk.advanceGenerationStatus(ChunkGenerationStatus.POPULATED);
                } finally {
                    for (int i = lockedChunks.length - 1; i >= 0; i--) {
                        lockedChunks[i].close();
                    }
                }
                populated.put(CloudChunk.key(chunkX, chunkZ), chunk);
            }
        }

        return populated;
    }

    /**
     * Compares primary and secondary block states for every block of every chunk.
     *
     * @param expected the expected chunks
     * @param actual   the chunks to compare
     */
    public static void assertSameBlocks(Long2ObjectMap<CloudChunk> expected, Long2ObjectMap<CloudChunk> actual) {
        long[] keys = expected.keySet().toLongArray();
        Arrays.sort(keys);
        for (long key : keys) {
            CloudChunk expectedChunk = expected.get(key);
            CloudChunk actualChunk = actual.get(key);
            assertNotNull(actualChunk, () -> "Missing chunk " + CloudChunk.fromKeyX(key) + "," + CloudChunk.fromKeyZ(key));
            int minHeight = expectedChunk.getLevel().getMinHeight();
            int maxHeight = expectedChunk.getLevel().getMaxHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minHeight; y < maxHeight; y++) {
                        assertPrimary(expectedChunk, actualChunk, x, y, z);
                        assertSecondary(expectedChunk, actualChunk, x, y, z);
                    }
                }
            }
        }
    }

    private static void assertPrimary(CloudChunk expectedChunk, CloudChunk actualChunk, int x, int y, int z) {
        assertEquals(expectedChunk.getBlockState(x, y, z, BlockLayer.PRIMARY),
                actualChunk.getBlockState(x, y, z, BlockLayer.PRIMARY),
                () -> "First primary difference at x=" + ((expectedChunk.getX() << 4) + x)
                        + " y=" + y + " z=" + ((expectedChunk.getZ() << 4) + z));
    }

    private static void assertSecondary(CloudChunk expectedChunk, CloudChunk actualChunk, int x, int y, int z) {
        assertEquals(expectedChunk.getBlockState(x, y, z, BlockLayer.SECONDARY),
                actualChunk.getBlockState(x, y, z, BlockLayer.SECONDARY),
                () -> "First secondary difference at x=" + ((expectedChunk.getX() << 4) + x)
                        + " y=" + y + " z=" + ((expectedChunk.getZ() << 4) + z));
    }

    /**
     * Compares sky and block light for every block of every chunk.
     *
     * @param expected the expected chunks
     * @param actual   the chunks to compare
     */
    public static void assertSameLight(Long2ObjectMap<CloudChunk> expected, Long2ObjectMap<CloudChunk> actual) {
        long[] keys = expected.keySet().toLongArray();
        Arrays.sort(keys);
        for (long key : keys) {
            CloudChunk expectedChunk = expected.get(key);
            CloudChunk actualChunk = actual.get(key);
            assertNotNull(actualChunk, () -> "Missing chunk " + CloudChunk.fromKeyX(key) + "," + CloudChunk.fromKeyZ(key));
            int minHeight = expectedChunk.getLevel().getMinHeight();
            int maxHeight = expectedChunk.getLevel().getMaxHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minHeight; y < maxHeight; y++) {
                        int absoluteX = (expectedChunk.getX() << 4) + x;
                        int absoluteZ = (expectedChunk.getZ() << 4) + z;
                        assertChannel(expectedChunk.getBlockLight(x, y, z), actualChunk.getBlockLight(x, y, z),
                                absoluteX, y, absoluteZ, "block");
                        assertChannel(expectedChunk.getSkyLight(x, y, z), actualChunk.getSkyLight(x, y, z),
                                absoluteX, y, absoluteZ, "sky");
                    }
                }
            }
        }
    }

    private static void assertChannel(int expected, int actual, int x, int y, int z, String channel) {
        assertEquals(expected, actual,
                () -> "First light difference at x=" + x + " y=" + y + " z=" + z + " channel=" + channel
                        + " expected=" + expected + " actual=" + actual);
    }
}
