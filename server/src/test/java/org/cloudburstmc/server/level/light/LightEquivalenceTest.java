package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.light.oracle.OracleLight;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightEquivalenceTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void fixtureGeneratesDeterministicBlocks() {
        LightTestChunks.assertSameBlocks(
                LightTestChunks.overworld(11L, -2, -2, 5),
                LightTestChunks.overworld(11L, -2, -2, 5));
    }

    @Test
    void initializationMatchesTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(11L, -2, -2, 5);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(11L, -2, -2, 5);
        initializeBoth(expected, actual);

        LightTestChunks.assertSameLight(expected, actual);
        assertHasSkyLight(actual);
        assertHasBlockLight(actual);
    }

    @Test
    void removingSourcesAndRoofsMatchesTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(11L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(11L, 0, 0, 3);
        initializeBoth(expected, actual);

        Vector3i center = openGroundCenter(actual, 1, 1);
        List<Vector3i> cluster = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                cluster.add(Vector3i.from(center.getX() + dx, center.getY(), center.getZ() + dz));
            }
        }
        List<Vector3i> roof = new ArrayList<>();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                roof.add(Vector3i.from(center.getX() + dx, center.getY() + 4, center.getZ() + dz));
            }
        }
        setBoth(expected, actual, cluster, BlockTypes.GLOWSTONE.getDefaultState());
        setBoth(expected, actual, roof, BlockStates.STONE);
        List<Vector3i> changed = new ArrayList<>(cluster);
        changed.addAll(roof);
        updateBoth(expected, actual, changed);
        LightTestChunks.assertSameLight(expected, actual);

        int block = blockLightAt(actual, center);
        int sky = skyLightAt(actual, center);
        assertEquals(15, block);

        setBoth(expected, actual, List.of(center), BlockStates.AIR);
        updateBoth(expected, actual, List.of(center));
        LightTestChunks.assertSameLight(expected, actual);
        LightLevels next = changedLights(actual, center, block, sky);
        block = next.block();
        sky = next.sky();

        setBoth(expected, actual, cluster, BlockStates.AIR);
        updateBoth(expected, actual, cluster);
        LightTestChunks.assertSameLight(expected, actual);
        next = changedLights(actual, center, block, sky);
        block = next.block();
        sky = next.sky();

        setBoth(expected, actual, roof, BlockStates.AIR);
        updateBoth(expected, actual, roof);
        LightTestChunks.assertSameLight(expected, actual);
        changedLights(actual, center, block, sky);
    }

    @Test
    void editsMatchTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(11L, 1, 1, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(11L, 1, 1, 3);
        initializeBoth(expected, actual);
        LightTestChunks.assertSameLight(expected, actual);

        BlockState bottomSlab = BlockTypes.OAK_SLAB.getDefaultState().withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        BlockState[] palette = {
                BlockTypes.GLOWSTONE.getDefaultState(),
                BlockTypes.TORCH.getDefaultState(),
                BlockStates.STONE,
                BlockTypes.GLASS.getDefaultState(),
                BlockTypes.WATER.getDefaultState(),
                BlockStates.AIR,
                bottomSlab
        };
        RandomGenerator random = new FastPRandom(3L);
        List<Vector3i> batch = new ArrayList<>(5);
        Vector3i glowstone = null;
        for (int i = 0; i < 40; i++) {
            int chunkX = 1 + random.nextInt(3);
            int chunkZ = 1 + random.nextInt(3);
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);
            if (i < 10) {
                if (random.nextBoolean()) {
                    localX = random.nextBoolean() ? 0 : 15;
                } else {
                    localZ = random.nextBoolean() ? 0 : 15;
                }
            }
            CloudChunk column = expected.get(CloudChunk.key(chunkX, chunkZ));
            int surface = column.getHighestBlock(localX, localZ);
            int y = Math.min(MAX_HEIGHT - 1, Math.max(MIN_HEIGHT, surface + random.nextInt(7) - 3));
            Vector3i position = Vector3i.from((chunkX << 4) + localX, y, (chunkZ << 4) + localZ);
            BlockState state = palette[random.nextInt(palette.length)];
            if (i == 39) {
                state = BlockTypes.GLOWSTONE.getDefaultState();
                glowstone = position;
            }
            setBoth(expected, actual, List.of(position), state);
            batch.add(position);
            if (batch.size() == 5) {
                updateBoth(expected, actual, batch);
                LightTestChunks.assertSameLight(expected, actual);
                batch.clear();
            }
        }

        assertNotNull(glowstone);
        assertEquals(15, blockLightAt(actual, glowstone));
    }

    @Test
    void unloadBoundaryMatchesTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(11L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(11L, 0, 0, 3);
        initializeBoth(expected, actual);
        LightTestChunks.assertSameLight(expected, actual);

        long removed = CloudChunk.key(0, 0);
        expected.remove(removed);
        actual.remove(removed);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, insideBoundary(0, 0));
        LightRegion region = new LightRegion(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, actual::get);
        assertTrue(region.unloaded(0, 0), "light propagation did not drain after unload");
        LightTestChunks.assertSameLight(expected, actual);
    }

    @Test
    void updateStaysInsideTheLockedNeighbourhood() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(7L, 0, 0, 5);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(7L, 0, 0, 5);
        initializeBoth(expected, actual);

        Vector3i center = openGroundCenter(actual, 2, 2);
        setBoth(expected, actual, List.of(center), BlockTypes.GLOWSTONE.getDefaultState());
        updateBoth(expected, actual, List.of(center));
        LightTestChunks.assertSameLight(expected, actual);
        assertEquals(15, blockLightAt(actual, center));
    }

    private static void initializeBoth(Long2ObjectMap<CloudChunk> expected, Long2ObjectMap<CloudChunk> actual) {
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }
        LightRegion region = new LightRegion(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, actual::get);
        for (long key : sortedKeys(actual)) {
            assertTrue(region.initialize(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key)),
                    () -> "light did not drain initializing chunk "
                            + CloudChunk.fromKeyX(key) + "," + CloudChunk.fromKeyZ(key));
        }
    }

    private static void updateBoth(Long2ObjectMap<CloudChunk> expected, Long2ObjectMap<CloudChunk> actual,
            Collection<Vector3i> changed) {
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, changed);
        LightRegion region = new LightRegion(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, actual::get);
        assertTrue(region.update(packed(changed)), "light propagation did not drain");
    }

    private static LongCollection packed(Collection<Vector3i> positions) {
        LongCollection packed = new LongArrayList(positions.size());
        for (Vector3i position : positions) {
            packed.add(LightPositions.pack(position.getX(), position.getY(), position.getZ()));
        }
        return packed;
    }

    private static List<Vector3i> insideBoundary(int chunkX, int chunkZ) {
        List<Vector3i> positions = new ArrayList<>();
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int y = MIN_HEIGHT; y < MAX_HEIGHT; y++) {
            for (int offset = 0; offset < 16; offset++) {
                positions.add(Vector3i.from(minX + offset, y, minZ));
                positions.add(Vector3i.from(minX + offset, y, minZ + 15));
                if (offset > 0 && offset < 15) {
                    positions.add(Vector3i.from(minX, y, minZ + offset));
                    positions.add(Vector3i.from(minX + 15, y, minZ + offset));
                }
            }
        }
        return positions;
    }

    private static Vector3i openGroundCenter(Long2ObjectMap<CloudChunk> chunks, int chunkX, int chunkZ) {
        CloudChunk chunk = chunks.get(CloudChunk.key(chunkX, chunkZ));
        assertNotNull(chunk);
        int top = chunk.getHighestBlock(8, 8);
        assertTrue(top + 5 < MAX_HEIGHT);
        return Vector3i.from((chunkX << 4) + 8, top + 1, (chunkZ << 4) + 8);
    }

    private static void setBoth(Long2ObjectMap<CloudChunk> expected, Long2ObjectMap<CloudChunk> actual,
            Collection<Vector3i> positions, BlockState state) {
        for (Vector3i position : positions) {
            long key = CloudChunk.key(position.getX() >> 4, position.getZ() >> 4);
            expected.get(key).setBlockState(position.getX() & 15, position.getY(), position.getZ() & 15,
                    BlockLayer.PRIMARY, state);
            actual.get(key).setBlockState(position.getX() & 15, position.getY(), position.getZ() & 15,
                    BlockLayer.PRIMARY, state);
        }
    }

    private static int blockLightAt(Long2ObjectMap<CloudChunk> chunks, Vector3i position) {
        CloudChunk chunk = chunks.get(CloudChunk.key(position.getX() >> 4, position.getZ() >> 4));
        assertNotNull(chunk);
        return chunk.getBlockLight(position.getX() & 15, position.getY(), position.getZ() & 15);
    }

    private static int skyLightAt(Long2ObjectMap<CloudChunk> chunks, Vector3i position) {
        CloudChunk chunk = chunks.get(CloudChunk.key(position.getX() >> 4, position.getZ() >> 4));
        assertNotNull(chunk);
        return chunk.getSkyLight(position.getX() & 15, position.getY(), position.getZ() & 15);
    }

    private static LightLevels changedLights(Long2ObjectMap<CloudChunk> chunks, Vector3i position, int block, int sky) {
        int nextBlock = blockLightAt(chunks, position);
        int nextSky = skyLightAt(chunks, position);
        assertTrue(nextBlock != block || nextSky != sky,
                () -> "Light under the roof did not change at " + position
                        + " (block=" + block + " sky=" + sky + ")");
        return new LightLevels(nextBlock, nextSky);
    }

    private static void assertHasSkyLight(Long2ObjectMap<CloudChunk> chunks) {
        boolean top = false;
        boolean below = false;
        outer:
        for (long key : sortedKeys(chunks)) {
            CloudChunk chunk = chunks.get(key);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getSkyLight(x, MAX_HEIGHT - 1, z) == 15) {
                        top = true;
                    }
                    for (int y = MIN_HEIGHT; y < MAX_HEIGHT - 1; y++) {
                        if (chunk.getSkyLight(x, y, z) == 15) {
                            below = true;
                        }
                        if (top && below) {
                            break outer;
                        }
                    }
                }
            }
        }
        assertTrue(top, "No sky light 15 in the top layer after initialization");
        assertTrue(below, "No sky light 15 below the top layer after initialization");
    }

    private static void assertHasBlockLight(Long2ObjectMap<CloudChunk> chunks) {
        for (long key : sortedKeys(chunks)) {
            CloudChunk chunk = chunks.get(key);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = MIN_HEIGHT; y < MAX_HEIGHT; y++) {
                        if (chunk.getBlockLight(x, y, z) > 0) {
                            return;
                        }
                    }
                }
            }
        }
        throw new AssertionError("No block light found after initialization");
    }

    private static long[] sortedKeys(Long2ObjectMap<CloudChunk> chunks) {
        long[] keys = chunks.keySet().toLongArray();
        Arrays.sort(keys);
        return keys;
    }

    private record LightLevels(int block, int sky) {
    }
}
