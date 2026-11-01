package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.light.oracle.OracleLight;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bounds the light engine's initialization cost, lock holds and backlog drain.
 */
class LightSpeedTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    private static final long SPEED_SEED = 5L;
    private static final int SPEED_WIDTH = 5;
    private static final int SPEED_INNER_OFFSET = 1;
    private static final int SPEED_INNER_WIDTH = 3;
    private static final int WARMUP_ORIGIN_X = 0;
    private static final int[] TIMED_ORIGIN_X = {100, 200, 300};
    private static final double SPEED_FACTOR = 5.0;

    private static final int HOLD_ORIGIN_X = 400;
    private static final int HOLD_WIDTH = 5;
    private static final long P99_HOLD_NANOS = 2_000_000L;

    private static final int BACKLOG_ORIGIN_X = 500;
    private static final int BACKLOG_WIDTH = 7;
    private static final long BACKLOG_MS_PER_CHUNK = 50L;
    private static final long BACKLOG_POLL_MS = 10L;

    @Test
    void initializationIsAtLeastFiveTimesFasterThanTheOracle() {
        timePair(WARMUP_ORIGIN_X);

        long oracleBest = Long.MAX_VALUE;
        long newBest = Long.MAX_VALUE;
        for (int originX : TIMED_ORIGIN_X) {
            long[] pair = timePair(originX);
            oracleBest = Math.min(oracleBest, pair[0]);
            newBest = Math.min(newBest, pair[1]);
        }

        System.out.println("oracle best ms=" + toMillis(oracleBest));
        System.out.println("new best ms=" + toMillis(newBest));
        System.out.println("speedup ratio=" + String.format("%.2f", oracleBest / (double) newBest));

        final long oracleFastest = oracleBest;
        final long newFastest = newBest;
        assertTrue(newFastest * SPEED_FACTOR <= oracleFastest,
                () -> "new " + toMillis(newFastest) + " ms vs oracle " + toMillis(oracleFastest) + " ms");
    }

    @Test
    void lockHoldsStayShort() {
        drainScheduler(LightTestChunks.overworld(SPEED_SEED, WARMUP_ORIGIN_X, 0, HOLD_WIDTH));

        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(SPEED_SEED, HOLD_ORIGIN_X, 0, HOLD_WIDTH);
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(chunks), pending::add);
        for (long key : sortedKeys(chunks)) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
        }
        drainScheduler(scheduler, pending, chunks);

        long[] holds = scheduler.holdNanosSamples();
        assertTrue(holds.length > 0, "expected lock hold samples");
        Arrays.sort(holds);
        long p99 = holds[Math.min(holds.length - 1, (int) Math.ceil(0.99 * holds.length) - 1)];
        long max = holds[holds.length - 1];

        System.out.println("p99 hold ms=" + toMillis(p99));
        System.out.println("max hold ms=" + toMillis(max));

        final long observedP99 = p99;
        final long observedMax = max;
        assertTrue(observedP99 <= P99_HOLD_NANOS,
                () -> "p99 hold " + toMillis(observedP99) + " ms exceeds 2.0 ms (max " + toMillis(observedMax) + " ms)");
    }

    @Test
    void backlogDrains() throws Exception {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(SPEED_SEED, BACKLOG_ORIGIN_X, 0, BACKLOG_WIDTH);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(SPEED_SEED, BACKLOG_ORIGIN_X, 0, BACKLOG_WIDTH);

        long[] keys = sortedKeys(actual);
        for (long key : keys) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }

        ForkJoinPool pool = new ForkJoinPool(2);
        try {
            LightScheduler scheduler = new LightScheduler(view(actual), pool::execute);
            long start = System.nanoTime();
            for (long key : keys) {
                scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
            }
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(keys.length * BACKLOG_MS_PER_CHUNK);
            while (!scheduler.isIdle() && System.nanoTime() < deadline) {
                Thread.sleep(BACKLOG_POLL_MS);
            }
            long elapsed = System.nanoTime() - start;
            System.out.println("backlog drain ms=" + toMillis(elapsed));
            assertTrue(scheduler.isIdle(), () -> "backlog did not drain, idle=false");
            LightTestChunks.assertSameLight(expected, actual);
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "pool did not stop");
        }
    }

    private static long[] timePair(int originX) {
        Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(SPEED_SEED, originX, 0, SPEED_WIDTH);
        Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(SPEED_SEED, originX, 0, SPEED_WIDTH);
        long[] inner = innerKeys(expected, originX, 0);

        long start = System.nanoTime();
        for (long key : inner) {
            OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
        }
        long oracleNanos = System.nanoTime() - start;

        LightRegion region = new LightRegion(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, actual::get,
                Integer.MAX_VALUE, null);
        start = System.nanoTime();
        for (long key : inner) {
            region.initialize(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
        }
        long newNanos = System.nanoTime() - start;
        return new long[]{oracleNanos, newNanos};
    }

    private static long[] innerKeys(Long2ObjectMap<CloudChunk> chunks, int originX, int originZ) {
        long[] keys = new long[SPEED_INNER_WIDTH * SPEED_INNER_WIDTH];
        int index = 0;
        for (int chunkX = originX + SPEED_INNER_OFFSET; chunkX < originX + SPEED_INNER_OFFSET + SPEED_INNER_WIDTH; chunkX++) {
            for (int chunkZ = originZ + SPEED_INNER_OFFSET; chunkZ < originZ + SPEED_INNER_OFFSET + SPEED_INNER_WIDTH; chunkZ++) {
                long key = CloudChunk.key(chunkX, chunkZ);
                if (chunks.containsKey(key)) {
                    keys[index++] = key;
                }
            }
        }
        Arrays.sort(keys, 0, index);
        return Arrays.copyOf(keys, index);
    }

    private static void drainScheduler(LightScheduler scheduler, List<Runnable> pending,
            Long2ObjectMap<CloudChunk> chunks) {
        for (int i = 0; i < 10_000; i++) {
            if (scheduler.isIdle() && pending.isEmpty()) {
                return;
            }
            if (pending.isEmpty()) {
                throw new AssertionError("Scheduler is not idle but no work was submitted");
            }
            List<Runnable> running = new ArrayList<>(pending);
            pending.clear();
            for (Runnable runnable : running) {
                runnable.run();
            }
        }
        throw new AssertionError("Scheduler did not drain");
    }

    private static void drainScheduler(Long2ObjectMap<CloudChunk> chunks) {
        List<Runnable> pending = new ArrayList<>();
        LightScheduler scheduler = new LightScheduler(view(chunks), pending::add);
        for (long key : sortedKeys(chunks)) {
            scheduler.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
        }
        drainScheduler(scheduler, pending, chunks);
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

    private static long[] sortedKeys(Long2ObjectMap<CloudChunk> chunks) {
        long[] keys = chunks.keySet().toLongArray();
        Arrays.sort(keys);
        return keys;
    }

    private static String toMillis(long nanos) {
        return String.format("%.1f", nanos / 1_000_000.0);
    }
}
