package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.light.oracle.OracleLight;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in generated-terrain benchmark comparing initialization with the reference engine.
 * Enable with {@code -Dcloudburst.lightBenchmark=true}.
 */
class LightSpeedTest {

    private static final String BENCHMARK_PROPERTY = "cloudburst.lightBenchmark";

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    private static final long SPEED_SEED = 5L;
    private static final int SPEED_WIDTH = 5;
    private static final int[] SPEED_ORIGINS = {100, 200, 300};
    private static final int SPEED_INNER_OFFSET = 1;
    private static final int SPEED_INNER_WIDTH = 3;

    private static final int BACKLOG_WIDTH = 7;
    private static final long BACKLOG_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(30);

    /**
     * Each distinct fixture is generated once. Tests copy their own chunks, so
     * no mutable chunk state is shared between tests.
     */
    private static Long2ObjectMap<CloudChunk> speedFixture;
    private static List<Long2ObjectMap<CloudChunk>> speedFixtures;
    private static Long2ObjectMap<CloudChunk> backlogFixture;

    @BeforeAll
    static void generateFixtures() {
        assumeTrue(Boolean.getBoolean(BENCHMARK_PROPERTY), "Opt-in lighting benchmark");
        Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT, Map.of("getSeed", SPEED_SEED));
        speedFixture = LightTestChunks.overworld(SPEED_SEED, 0, 0, SPEED_WIDTH, level);
        speedFixtures = new ArrayList<>(SPEED_ORIGINS.length);

        for (int origin : SPEED_ORIGINS) {
            speedFixtures.add(LightTestChunks.overworld(SPEED_SEED, origin, origin, SPEED_WIDTH, level));
        }

        backlogFixture = LightTestChunks.overworld(SPEED_SEED, 0, 0, BACKLOG_WIDTH, level);
    }

    @Test
    void initializationMatchesTheOracleAndReportsTiming() {
        timePair(speedFixtures.getFirst(), SPEED_ORIGINS[0]);

        double[] ratios = new double[SPEED_ORIGINS.length];

        for (int i = 0; i < SPEED_ORIGINS.length; i++) {
            long[] pair = timePair(speedFixtures.get(i), SPEED_ORIGINS[i]);
            ratios[i] = pair[0] / (double) pair[1];
            System.out.println("origin " + SPEED_ORIGINS[i] + " oracle ms=" + toMillis(pair[0])
                    + " engine ms=" + toMillis(pair[1]) + " ratio=" + String.format("%.2f", ratios[i]));
        }

        Arrays.sort(ratios);
        System.out.println("median speedup ratio=" + String.format("%.2f", ratios[ratios.length / 2]));
    }

    @Test
    void propagationHoldsStaySliced() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.copy(speedFixture);
        LightTestTasks tasks = new LightTestTasks();
        List<Long> holdNanos = new ArrayList<>();
        List<Integer> holdSteps = new ArrayList<>();
        try (LightScheduler scheduler = new LightScheduler(view(chunks), tasks, LightScheduler.SLICE_STEPS, (nanos, steps) -> {
            holdNanos.add(nanos);
            holdSteps.add(steps);
        })) {
            for (long key : sortedKeys(chunks)) {
                scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            }

            tasks.drain(scheduler);
        }

        long[] holds = holdNanos.stream().mapToLong(Long::longValue).toArray();
        int[] steps = holdSteps.stream().mapToInt(Integer::intValue).toArray();
        assertTrue(holds.length > 0, "expected lock hold samples");
        assertEquals(holds.length, steps.length, "hold durations and step counts diverged");
        boolean sawSeedHold = false;
        boolean sawFullSlice = false;

        for (int step : steps) {
            assertTrue(step >= 0 && step <= LightScheduler.SLICE_STEPS,
                    () -> "hold ran " + step + " propagation steps, over the " + LightScheduler.SLICE_STEPS
                            + " budget");
            sawSeedHold |= step == 0;
            sawFullSlice |= step == LightScheduler.SLICE_STEPS;
        }

        assertTrue(sawSeedHold, "expected seed holds to record 0 steps");
        assertTrue(sawFullSlice, "expected at least one hold to run the full " + LightScheduler.SLICE_STEPS
                + " step budget");
        Arrays.sort(holds);
        long p99 = holds[(int) Math.ceil(0.99 * holds.length) - 1];
        long max = holds[holds.length - 1];

        System.out.println("p99 hold ms=" + toMillis(p99));
        System.out.println("max hold ms=" + toMillis(max));
    }

    @Test
    void backlogDrains() {
        // Warms the lane and the block registry lookups before the timed drain.
        drainScheduler(LightTestChunks.copy(speedFixture));

        Long2ObjectMap<CloudChunk> expected = LightTestChunks.copy(backlogFixture);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.copy(backlogFixture);

        long[] keys = sortedKeys(actual);

        for (long key : keys) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }

        try (ForkJoinPool pool = new ForkJoinPool(2);
             LightScheduler scheduler = new LightScheduler(view(actual), task -> CompletableFuture.runAsync(task, pool))) {
            long start = System.nanoTime();

            for (long key : keys) {
                scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            }

            long deadline = start + BACKLOG_BUDGET_NANOS;

            while (!scheduler.isIdle() && System.nanoTime() < deadline) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }

            scheduler.checkFailure();
            long elapsed = System.nanoTime() - start;
            System.out.println("backlog drain ms=" + toMillis(elapsed));
            assertTrue(scheduler.isIdle(), () -> "backlog did not drain after " + toMillis(elapsed)
                    + " ms (budget " + toMillis(BACKLOG_BUDGET_NANOS) + " ms)");
            LightTestChunks.assertSameLight(expected, actual);
        }
    }

    private static long[] timePair(Long2ObjectMap<CloudChunk> fixture, int origin) {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.copy(fixture);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.copy(fixture);
        long[] inner = innerKeys(expected, origin, origin);

        long start = System.nanoTime();

        for (long key : inner) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }

        long oracleNanos = System.nanoTime() - start;

        LightRegion region = new LightRegion(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, actual::get,
                Integer.MAX_VALUE, null);
        start = System.nanoTime();

        for (long key : inner) {
            LightTestTasks.drain(region.initialize(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key)));
        }

        long engineNanos = System.nanoTime() - start;

        // The timed window lights only the inner square, whose border arrivals
        // depend on seed order on each side. Seeding the ring afterward, off
        // the clock, completes both fixtures, so the comparison below covers
        // the timed work in a fully seeded fixture where both sides agree.
        long[] ring = ringKeys(expected, inner);

        for (long key : ring) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }

        for (long key : ring) {
            LightTestTasks.drain(region.initialize(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key)));
        }

        LightTestChunks.assertSameLight(expected, actual);
        return new long[]{oracleNanos, engineNanos};
    }

    private static long[] innerKeys(Long2ObjectMap<CloudChunk> chunks, int originX, int originZ) {
        long[] keys = new long[SPEED_INNER_WIDTH * SPEED_INNER_WIDTH];
        int index = 0;

        for (int chunkX = originX + SPEED_INNER_OFFSET;
             chunkX < originX + SPEED_INNER_OFFSET + SPEED_INNER_WIDTH; chunkX++) {
            for (int chunkZ = originZ + SPEED_INNER_OFFSET;
                 chunkZ < originZ + SPEED_INNER_OFFSET + SPEED_INNER_WIDTH; chunkZ++) {
                long key = CloudChunk.key(chunkX, chunkZ);

                if (chunks.containsKey(key)) {
                    keys[index++] = key;
                }
            }
        }

        Arrays.sort(keys, 0, index);
        return Arrays.copyOf(keys, index);
    }

    private static long[] ringKeys(Long2ObjectMap<CloudChunk> chunks, long[] inner) {
        long[] all = sortedKeys(chunks);
        long[] ring = new long[all.length - inner.length];
        int index = 0;

        for (long key : all) {
            if (Arrays.binarySearch(inner, key) < 0) {
                ring[index++] = key;
            }
        }

        return Arrays.copyOf(ring, index);
    }

    private static void drainScheduler(Long2ObjectMap<CloudChunk> chunks) {
        LightTestTasks tasks = new LightTestTasks();
        try (LightScheduler scheduler = new LightScheduler(view(chunks), tasks)) {
            for (long key : sortedKeys(chunks)) {
                scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            }

            tasks.drain(scheduler);
        }
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
            public @Nullable CloudChunk getChunkForLighting(int chunkX, int chunkZ) {
                return chunks.get(CloudChunk.key(chunkX, chunkZ));
            }
        };
    }

    private static long[] sortedKeys(Long2ObjectMap<CloudChunk> chunks) {
        long[] keys = chunks.keySet().toLongArray();
        Arrays.sort(keys);
        return keys;
    }

    private static String toMillis(long nanos) {
        return String.format("%.1f", nanos / 1_000_000.0);
    }
}
