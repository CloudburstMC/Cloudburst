package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.light.oracle.OracleLight;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LightSchedulerTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void slicedRegionMatchesTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(9L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(9L, 0, 0, 3);
        LongCollection holds = new LongArrayList();
        LightRegion region = new LightRegion(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, actual::get, 64,
                holds::add);
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                    expected.get(key));
            region.initialize(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
        }
        LightTestChunks.assertSameLight(expected, actual);
        assertTrue(holds.size() > 9, "expected several slices per chunk");

        Vector3i glowstone = openGroundCenter(actual, 1, 1);
        setBoth(expected, actual, List.of(glowstone), BlockTypes.GLOWSTONE.getDefaultState());
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, List.of(glowstone));
        region.update(packed(List.of(glowstone)));
        LightTestChunks.assertSameLight(expected, actual);
    }

    @Test
    void chunkLoadOnlySubmitsWork() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(chunks), pending::add);

        scheduler.chunkLoaded(0, 0);

        assertEquals(1, pending.size());
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 0);
        for (Runnable runnable : new ArrayList<>(pending)) {
            pending.clear();
            runnable.run();
        }
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    @Test
    void drainedLaneMatchesTheOracle() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(5L, 0, 0, 5);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(5L, 0, 0, 5);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(actual), pending::add);

        long[] keys = sortedKeys(expected);
        for (long key : keys) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                    expected.get(key));
        }

        List<Vector3i> edits = edits(expected, 20);
        LongCollection packed = packed(edits);
        setBoth(expected, actual, edits);
        scheduler.blocksChanged(packed);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, edits);

        long removed = CloudChunk.key(0, 0);
        expected.remove(removed);
        actual.remove(removed);
        scheduler.chunkUnloaded(0, 0);
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, insideBoundary(0, 0));

        drain(scheduler, pending);
        LightTestChunks.assertSameLight(expected, actual);
        assertTrue(scheduler.isIdle());
        assertTrue(scheduler.maxHoldNanos() > 0);
        assertTrue(scheduler.holdNanosSamples().length > 0);
    }

    @Test
    void blockChangeBetweenSlicesStillDrainsToTheOracleResult() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(9L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(9L, 0, 0, 3);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(actual), pending::add);

        for (long key : sortedKeys(expected)) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                    expected.get(key));
        }

        Vector3i glowstone = openGroundCenter(actual, 1, 1);
        long glowKey = CloudChunk.key(glowstone.getX() >> 4, glowstone.getZ() >> 4);
        BlockState original = actual.get(glowKey).getBlockState(glowstone.getX() & 15,
                glowstone.getY(), glowstone.getZ() & 15, BlockLayer.PRIMARY);
        setBoth(expected, actual, List.of(glowstone), BlockTypes.GLOWSTONE.getDefaultState());
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, List.of(glowstone));

        // Revert the actual edit: the hook below applies it after the first slice.
        actual.get(glowKey).setBlockState(glowstone.getX() & 15, glowstone.getY(),
                glowstone.getZ() & 15, BlockLayer.PRIMARY, original);

        AtomicBoolean once = new AtomicBoolean();
        scheduler.afterSliceForTest(() -> {
            if (once.compareAndSet(false, true)) {
                setBoth(null, actual, List.of(glowstone), BlockTypes.GLOWSTONE.getDefaultState());
                scheduler.blocksChanged(packed(List.of(glowstone)));
            }
        });

        drain(scheduler, pending);
        LightTestChunks.assertSameLight(expected, actual);
    }

    @Test
    void editBetweenSeedBandsStillDrainsToTheOracleResult() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(9L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(9L, 0, 0, 3);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(actual), pending::add);

        for (long key : sortedKeys(expected)) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                    expected.get(key));
        }

        Vector3i glowstone = openGroundCenter(actual, 1, 1);
        long glowKey = CloudChunk.key(glowstone.getX() >> 4, glowstone.getZ() >> 4);
        BlockState original = actual.get(glowKey).getBlockState(glowstone.getX() & 15,
                glowstone.getY(), glowstone.getZ() & 15, BlockLayer.PRIMARY);
        setBoth(expected, actual, List.of(glowstone), BlockTypes.GLOWSTONE.getDefaultState());
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, List.of(glowstone));

        // Revert the actual edit: the hook below applies it after the first seed band.
        actual.get(glowKey).setBlockState(glowstone.getX() & 15, glowstone.getY(),
                glowstone.getZ() & 15, BlockLayer.PRIMARY, original);

        AtomicBoolean once = new AtomicBoolean();
        scheduler.afterSeedBandForTest(() -> {
            if (once.compareAndSet(false, true)) {
                setBoth(null, actual, List.of(glowstone), BlockTypes.GLOWSTONE.getDefaultState());
                scheduler.blocksChanged(packed(List.of(glowstone)));
            }
        });

        drain(scheduler, pending);
        LightTestChunks.assertSameLight(expected, actual);
    }

    @Test
    void largeEditSeedsInBandsAndDrainsToTheOracleResult() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(5L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(5L, 0, 0, 3);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(actual), pending::add);

        for (long key : sortedKeys(expected)) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                    expected.get(key));
        }
        drain(scheduler, pending);

        // More edits in one chunk than one seed band holds.
        List<Vector3i> edits = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = actual.get(CloudChunk.key(1, 1)).getHighestBlock(x, z);
                edits.add(Vector3i.from(16 + x, Math.min(MAX_HEIGHT - 1, top + 1), 16 + z));
                edits.add(Vector3i.from(16 + x, Math.min(MAX_HEIGHT - 1, top + 2), 16 + z));
            }
        }
        assertTrue(edits.size() > LightRegion.UPDATE_BAND_POSITIONS);
        setBoth(expected, actual, edits, BlockTypes.GLOWSTONE.getDefaultState());
        OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, edits);
        scheduler.blocksChanged(packed(edits));

        AtomicInteger seedBands = new AtomicInteger();
        Runnable[] hook = new Runnable[1];
        hook[0] = () -> {
            seedBands.incrementAndGet();
            scheduler.afterSeedBandForTest(hook[0]);
        };
        scheduler.afterSeedBandForTest(hook[0]);

        drain(scheduler, pending);
        assertEquals(LightRegion.updateBands(edits.size()), seedBands.get());
        LightTestChunks.assertSameLight(expected, actual);
    }

    @Test
    void unloadBetweenSlicesSkipsTheAbsentChunk() {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(9L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(9L, 0, 0, 3);
        long removed = CloudChunk.key(1, 1);
        expected.remove(removed);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(actual), pending::add, 64);

        long[] keys = sortedKeys(actual);
        for (long key : keys) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
        }
        for (long key : sortedKeys(expected)) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                    expected.get(key));
        }

        AtomicInteger slices = new AtomicInteger();
        AtomicBoolean unloaded = new AtomicBoolean();
        Runnable[] hook = new Runnable[1];
        hook[0] = () -> {
            slices.incrementAndGet();
            if (unloaded.compareAndSet(false, true)) {
                actual.remove(removed);
            }
            scheduler.afterSliceForTest(hook[0]);
        };
        scheduler.afterSliceForTest(hook[0]);

        // One item per run: the first pending runnable is the (0, 0) load.
        assertEquals(1, pending.size());
        List<Runnable> first = new ArrayList<>(pending);
        pending.clear();
        for (Runnable runnable : first) {
            runnable.run();
        }
        assertTrue(unloaded.get(), "hook did not run");
        assertTrue(slices.get() > 1,
                () -> "expected the first unit to span several slices, got " + slices.get());
        drain(scheduler, pending);
        LightTestChunks.assertSameLight(expected, actual);
    }

    @Test
    void rejectedSubmissionLeavesTheLaneUsable() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        AtomicBoolean rejected = new AtomicBoolean();
        LightScheduler scheduler = new LightScheduler(view(chunks), runnable -> {
            if (rejected.compareAndSet(false, true)) {
                throw new RejectedExecutionException("test");
            }
            pending.add(runnable);
        });

        scheduler.chunkLoaded(0, 0);
        assertTrue(scheduler.isIdle());
        assertTrue(pending.isEmpty());

        scheduler.chunkLoaded(0, 0);
        assertEquals(1, pending.size());
        drain(scheduler, pending);
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    @Test
    void onlyOneRunnerIsInFlight() {
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(5L, 0, 0, 3);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(actual), pending::add);

        scheduler.chunkLoaded(0, 0);
        scheduler.chunkLoaded(0, 1);
        scheduler.chunkLoaded(0, 2);
        assertEquals(1, pending.size());

        drain(scheduler, pending);
        assertTrue(scheduler.isIdle());
    }

    @Test
    void rejectedResubmitLeavesLaneIdleAndUsable() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        AtomicInteger submits = new AtomicInteger();
        LightScheduler scheduler = new LightScheduler(view(chunks), runnable -> {
            if (submits.getAndIncrement() == 1) {
                throw new RejectedExecutionException("test");
            }
            pending.add(runnable);
        });

        scheduler.chunkLoaded(0, 0);
        scheduler.chunkLoaded(0, 0);
        assertEquals(1, pending.size());

        List<Runnable> running = new ArrayList<>(pending);
        pending.clear();
        for (Runnable runnable : running) {
            runnable.run();
        }
        assertTrue(scheduler.isIdle());
        assertTrue(pending.isEmpty());

        scheduler.chunkLoaded(0, 0);
        assertEquals(1, pending.size());
        drain(scheduler, pending);
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    @Test
    void concurrentSubmissionsDrainToTheOracleResult() throws Exception {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(5L, 0, 0, 3);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(5L, 0, 0, 3);
        List<Vector3i> edits = edits(expected, 10);
        setBoth(expected, actual, edits);
        LongCollection packedEdits = packed(edits);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        boolean terminated = false;
        try {
            LightScheduler scheduler = new LightScheduler(view(actual), executor::execute);

            Thread submitter = new Thread(() -> scheduler.blocksChanged(packedEdits));
            submitter.start();
            for (long key : sortedKeys(expected)) {
                scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            }
            submitter.join(10_000);
            assertFalse(submitter.isAlive(), "submitter did not finish");

            for (long key : sortedKeys(expected)) {
                OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get,
                        expected.get(key));
            }
            OracleLight.update(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, edits);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!scheduler.isIdle() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertTrue(scheduler.isIdle(), "scheduler did not drain");
            LightTestChunks.assertSameLight(expected, actual);
        } finally {
            executor.shutdownNow();
            terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        }
        assertTrue(terminated, "executor did not stop");
    }

    @Test
    void errorInUnitKeepsLaneUsable() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(chunks), pending::add);

        scheduler.chunkLoaded(0, 0);
        scheduler.chunkLoaded(0, 0);
        AtomicBoolean thrown = new AtomicBoolean();
        scheduler.afterSliceForTest(() -> {
            if (thrown.compareAndSet(false, true)) {
                throw new AssertionError("test");
            }
        });

        drain(scheduler, pending);
        assertTrue(scheduler.isIdle());
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    @Test
    void laneFailureKeepsLaneUsable() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(chunks), pending::add);

        scheduler.chunkLoaded(0, 0);
        scheduler.chunkLoaded(0, 0);
        // Thrown from the new seam after the merge and before process: at the
        // same program point the pre-fix lane left unguarded.
        scheduler.beforeItemForTest(() -> {
            throw new AssertionError("test");
        });

        drain(scheduler, pending);
        assertTrue(scheduler.isIdle());
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    @Test
    void nonRejectedSubmitFailureKeepsWorkAndRestarts() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        AtomicBoolean thrown = new AtomicBoolean();
        LightScheduler scheduler = new LightScheduler(view(chunks), runnable -> {
            if (thrown.compareAndSet(false, true)) {
                throw new IllegalStateException("test");
            }
            pending.add(runnable);
        });

        try {
            scheduler.chunkLoaded(0, 0);
            fail("expected the submit failure to propagate");
        } catch (IllegalStateException expected) {
            // The flag is cleared with queued work kept.
        }
        assertFalse(scheduler.isIdle(), "queued work must be kept");
        assertTrue(pending.isEmpty());

        scheduler.chunkLoaded(0, 0);
        assertEquals(1, pending.size());
        drain(scheduler, pending);
        assertTrue(scheduler.isIdle());
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    @Test
    void nonRejectedResubmitFailureKeepsWorkAndRestarts() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        AtomicInteger submits = new AtomicInteger();
        LightScheduler scheduler = new LightScheduler(view(chunks), runnable -> {
            if (submits.getAndIncrement() == 1) {
                throw new IllegalStateException("test");
            }
            pending.add(runnable);
        });

        scheduler.chunkLoaded(0, 0);
        scheduler.chunkLoaded(0, 0);
        assertEquals(1, pending.size());

        List<Runnable> running = new ArrayList<>(pending);
        pending.clear();
        try {
            for (Runnable runnable : running) {
                runnable.run();
            }
            fail("expected the resubmit failure to propagate");
        } catch (IllegalStateException expected) {
            // The flag is cleared with queued work kept.
        }
        assertFalse(scheduler.isIdle(), "queued work must be kept");
        assertTrue(pending.isEmpty());

        scheduler.chunkLoaded(0, 0);
        assertEquals(1, pending.size());
        drain(scheduler, pending);
        assertTrue(scheduler.isIdle());
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    private static void drain(LightScheduler scheduler, List<Runnable> pending) {
        for (int i = 0; i < 1000; i++) {
            if (scheduler.isIdle() && pending.isEmpty()) {
                return;
            }
            if (pending.isEmpty()) {
                fail("Scheduler is not idle but no work was submitted");
            }
            List<Runnable> running = new ArrayList<>(pending);
            pending.clear();
            for (Runnable runnable : running) {
                runnable.run();
            }
        }
        fail("Scheduler did not drain");
    }

    private static LightLevelView view(Long2ObjectMap<CloudChunk> chunks) {
        return new LightLevelView() {
            @Override
            public BlockRegistry registry() {
                return REGISTRY;
            }

            @Override
            public int minHeight() {
                return MIN_HEIGHT;
            }

            @Override
            public int maxHeight() {
                return MAX_HEIGHT;
            }

            @Override
            public boolean hasSkyLight() {
                return true;
            }

            @Override
            public @Nullable CloudChunk loadedChunk(int chunkX, int chunkZ) {
                return chunks.get(CloudChunk.key(chunkX, chunkZ));
            }
        };
    }

    private static void assertSkyTop(CloudChunk chunk, int light) {
        assertNotNull(chunk);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int localX = x;
                int localZ = z;
                assertEquals(light, chunk.getSkyLight(localX, MAX_HEIGHT - 1, localZ),
                        () -> "Sky light at x=" + localX + " z=" + localZ);
            }
        }
    }

    private static List<Vector3i> edits(Long2ObjectMap<CloudChunk> chunks, int count) {
        List<Vector3i> edits = new ArrayList<>(count);
        long[] keys = sortedKeys(chunks);
        for (int i = 0; i < count; i++) {
            long key = keys[i % keys.length];
            int chunkX = CloudChunk.fromKeyX(key);
            int chunkZ = CloudChunk.fromKeyZ(key);
            int localX = (i * 7) % 16;
            int localZ = (i * 11) % 16;
            CloudChunk column = chunks.get(key);
            int y = Math.min(MAX_HEIGHT - 1,
                    Math.max(MIN_HEIGHT, column.getHighestBlock(localX, localZ) + 1));
            edits.add(Vector3i.from((chunkX << 4) + localX, y, (chunkZ << 4) + localZ));
        }
        return edits;
    }

    private static void setBoth(@Nullable Long2ObjectMap<CloudChunk> expected,
            @Nullable Long2ObjectMap<CloudChunk> actual, List<Vector3i> positions, BlockState state) {
        for (Vector3i position : positions) {
            long key = CloudChunk.key(position.getX() >> 4, position.getZ() >> 4);
            if (expected != null) {
                expected.get(key).setBlockState(position.getX() & 15, position.getY(),
                        position.getZ() & 15, BlockLayer.PRIMARY, state);
            }
            if (actual != null) {
                actual.get(key).setBlockState(position.getX() & 15, position.getY(),
                        position.getZ() & 15, BlockLayer.PRIMARY, state);
            }
        }
    }

    private static void setBoth(Long2ObjectMap<CloudChunk> expected, Long2ObjectMap<CloudChunk> actual,
            List<Vector3i> positions) {
        BlockState[] palette = {
                BlockTypes.GLOWSTONE.getDefaultState(),
                BlockTypes.TORCH.getDefaultState(),
                BlockTypes.STONE.getDefaultState(),
                BlockTypes.GLASS.getDefaultState(),
        };
        for (int i = 0; i < positions.size(); i++) {
            setBoth(expected, actual, List.of(positions.get(i)), palette[i % palette.length]);
        }
    }

    private static LongCollection packed(List<Vector3i> positions) {
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

    private static long[] sortedKeys(Long2ObjectMap<CloudChunk> chunks) {
        long[] keys = chunks.keySet().toLongArray();
        Arrays.sort(keys);
        return keys;
    }
}
