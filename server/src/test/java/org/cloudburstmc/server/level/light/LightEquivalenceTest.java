package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.level.light.oracle.OracleLight;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongFunction;
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
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }
        for (long key : sortedKeys(actual)) {
            initializeNew(actual, actual.get(key));
        }

        LightTestChunks.assertSameLight(expected, actual);
        assertHasSkyLight(actual);
        assertHasBlockLight(actual);
    }

    @Test
    void removingSourcesAndRoofsMatchesTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(11L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(11L, 0, 0, 3);
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }
        for (long key : sortedKeys(actual)) {
            initializeNew(actual, actual.get(key));
        }

        Vector3i center = openGroundCenter(actual);
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
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, changed);
        updateNew(actual, changed);
        LightTestChunks.assertSameLight(expected, actual);
        assertEquals(15, blockLightAt(actual, center));

        int block = blockLightAt(actual, center);
        int sky = skyLightAt(actual, center);

        setBoth(expected, actual, List.of(center), BlockStates.AIR);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, List.of(center));
        updateNew(actual, List.of(center));
        LightTestChunks.assertSameLight(expected, actual);
        int[] next = changedLights(actual, center, block, sky);
        block = next[0];
        sky = next[1];

        setBoth(expected, actual, cluster, BlockStates.AIR);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, cluster);
        updateNew(actual, cluster);
        LightTestChunks.assertSameLight(expected, actual);
        next = changedLights(actual, center, block, sky);
        block = next[0];
        sky = next[1];

        setBoth(expected, actual, roof, BlockStates.AIR);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, roof);
        updateNew(actual, roof);
        LightTestChunks.assertSameLight(expected, actual);
        changedLights(actual, center, block, sky);
    }

    @Test
    void editsMatchTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(11L, 1, 1, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(11L, 1, 1, 3);
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }
        for (long key : sortedKeys(actual)) {
            initializeNew(actual, actual.get(key));
        }
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
                OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, batch);
                updateNew(actual, batch);
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
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }
        for (long key : sortedKeys(actual)) {
            initializeNew(actual, actual.get(key));
        }
        LightTestChunks.assertSameLight(expected, actual);

        long removed = CloudChunk.key(0, 0);
        expected.remove(removed);
        actual.remove(removed);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, insideBoundary(0, 0));
        boundaryNew(actual, 0, 0);
        LightTestChunks.assertSameLight(expected, actual);
    }

    private static void initializeNew(Long2ObjectMap<CloudChunk> chunks, CloudChunk chunk) {
        List<CloudChunk> surrounding = new ArrayList<>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                CloudChunk neighbour = chunks.get(CloudChunk.key(chunk.getX() + dx, chunk.getZ() + dz));
                if (neighbour != null) {
                    surrounding.add(neighbour);
                }
            }
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            LongFunction<LockedChunk> views = views(locked);
            LightStateCache states = new LightStateCache(REGISTRY);
            LightPropagation block = new LightPropagation(LightChannel.BLOCK);
            ChunkLightAccess blockAccess = new ChunkLightAccess(LightChannel.BLOCK, MIN_HEIGHT, MAX_HEIGHT, states, views);
            BlockLightEngine.initialize(blockAccess, chunk.getX(), chunk.getZ(), block);
            assertTrue(block.run(blockAccess, Integer.MAX_VALUE));
            LightPropagation sky = new LightPropagation(LightChannel.SKY);
            ChunkLightAccess skyAccess = new ChunkLightAccess(LightChannel.SKY, MIN_HEIGHT, MAX_HEIGHT, states, views);
            SkyLightEngine.initialize(skyAccess, chunk.getX(), chunk.getZ(), sky);
            assertTrue(sky.run(skyAccess, Integer.MAX_VALUE));
        } finally {
            for (int i = locked.length - 1; i >= 0; i--) {
                locked[i].close();
            }
        }
    }

    private static void updateNew(Long2ObjectMap<CloudChunk> chunks, Collection<Vector3i> changed) {
        Set<CloudChunk> surrounding = new TreeSet<>();
        for (Vector3i position : changed) {
            int chunkX = position.getX() >> 4;
            int chunkZ = position.getZ() >> 4;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    CloudChunk neighbour = chunks.get(CloudChunk.key(chunkX + dx, chunkZ + dz));
                    if (neighbour != null) {
                        surrounding.add(neighbour);
                    }
                }
            }
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            LongFunction<LockedChunk> views = views(locked);
            LightStateCache states = new LightStateCache(REGISTRY);
            ChunkLightAccess blockAccess = new ChunkLightAccess(LightChannel.BLOCK, MIN_HEIGHT, MAX_HEIGHT, states, views);
            ChunkLightAccess skyAccess = new ChunkLightAccess(LightChannel.SKY, MIN_HEIGHT, MAX_HEIGHT, states, views);
            LightPropagation block = new LightPropagation(LightChannel.BLOCK);
            LightPropagation sky = new LightPropagation(LightChannel.SKY);
            for (Vector3i position : changed) {
                long packed = LightPositions.pack(position.getX(), position.getY(), position.getZ());
                block.invalidate(blockAccess, packed);
                sky.invalidate(skyAccess, packed);
            }
            assertTrue(block.run(blockAccess, Integer.MAX_VALUE));
            assertTrue(sky.run(skyAccess, Integer.MAX_VALUE));
        } finally {
            for (int i = locked.length - 1; i >= 0; i--) {
                locked[i].close();
            }
        }
    }

    private static void boundaryNew(Long2ObjectMap<CloudChunk> chunks, int chunkX, int chunkZ) {
        List<CloudChunk> surrounding = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                CloudChunk neighbour = chunks.get(CloudChunk.key(chunkX + dx, chunkZ + dz));
                if (neighbour != null) {
                    surrounding.add(neighbour);
                }
            }
        }
        LockedChunk[] locked = lockSorted(surrounding);
        try {
            LongFunction<LockedChunk> views = views(locked);
            LightStateCache states = new LightStateCache(REGISTRY);
            ChunkLightAccess blockAccess = new ChunkLightAccess(LightChannel.BLOCK, MIN_HEIGHT, MAX_HEIGHT, states, views);
            ChunkLightAccess skyAccess = new ChunkLightAccess(LightChannel.SKY, MIN_HEIGHT, MAX_HEIGHT, states, views);
            LightPropagation block = new LightPropagation(LightChannel.BLOCK);
            LightPropagation sky = new LightPropagation(LightChannel.SKY);
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            for (int y = MIN_HEIGHT; y < MAX_HEIGHT; y++) {
                for (int i = 0; i < 16; i++) {
                    invalidateIfDim(blockAccess, block, minX - 1, y, minZ + i);
                    invalidateIfDim(blockAccess, block, minX + 16, y, minZ + i);
                    invalidateIfDim(blockAccess, block, minX + i, y, minZ - 1);
                    invalidateIfDim(blockAccess, block, minX + i, y, minZ + 16);
                    invalidateIfDim(skyAccess, sky, minX - 1, y, minZ + i);
                    invalidateIfDim(skyAccess, sky, minX + 16, y, minZ + i);
                    invalidateIfDim(skyAccess, sky, minX + i, y, minZ - 1);
                    invalidateIfDim(skyAccess, sky, minX + i, y, minZ + 16);
                }
            }
            assertTrue(block.run(blockAccess, Integer.MAX_VALUE));
            assertTrue(sky.run(skyAccess, Integer.MAX_VALUE));
        } finally {
            for (int i = locked.length - 1; i >= 0; i--) {
                locked[i].close();
            }
        }
    }

    private static void invalidateIfDim(ChunkLightAccess access, LightPropagation propagation, int x, int y, int z) {
        if (access.isLoaded(x, z)) {
            int light = access.light(x, y, z);
            if (light >= 1 && light <= 14) {
                propagation.invalidate(access, LightPositions.pack(x, y, z));
            }
        }
    }

    private static LockedChunk[] lockSorted(Collection<CloudChunk> chunks) {
        return chunks.stream().sorted().map(CloudChunk::lockForWrite).toArray(LockedChunk[]::new);
    }

    private static LongFunction<LockedChunk> views(LockedChunk[] locked) {
        Long2ObjectOpenHashMap<LockedChunk> byKey = new Long2ObjectOpenHashMap<>(locked.length);
        for (LockedChunk view : locked) {
            byKey.put(CloudChunk.key(view.getX(), view.getZ()), view);
        }
        return byKey::get;
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

    private static Vector3i openGroundCenter(Long2ObjectMap<CloudChunk> chunks) {
        CloudChunk chunk = chunks.get(CloudChunk.key(1, 1));
        assertNotNull(chunk);
        int top = chunk.getHighestBlock(8, 8);
        assertTrue(top + 5 < MAX_HEIGHT);
        return Vector3i.from((1 << 4) + 8, top + 1, (1 << 4) + 8);
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

    private static int[] changedLights(Long2ObjectMap<CloudChunk> chunks, Vector3i position, int block, int sky) {
        int nextBlock = blockLightAt(chunks, position);
        int nextSky = skyLightAt(chunks, position);
        assertTrue(nextBlock != block || nextSky != sky,
                () -> "Light under the roof did not change at " + position
                        + " (block=" + block + " sky=" + sky + ")");
        return new int[]{nextBlock, nextSky};
    }

    private static void assertHasSkyLight(Long2ObjectMap<CloudChunk> chunks) {
        for (long key : sortedKeys(chunks)) {
            CloudChunk chunk = chunks.get(key);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getSkyLight(x, MAX_HEIGHT - 1, z) == 15) {
                        return;
                    }
                }
            }
        }
        throw new AssertionError("No sky light 15 found after initialization");
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
}
