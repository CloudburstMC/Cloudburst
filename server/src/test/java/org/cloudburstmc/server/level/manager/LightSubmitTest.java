package org.cloudburstmc.server.level.manager;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.light.LightLevelView;
import org.cloudburstmc.server.level.light.LightScheduler;
import org.cloudburstmc.server.level.light.LightTestChunks;
import org.cloudburstmc.server.level.light.oracle.OracleLight;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LightSubmitTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void synchronousRejectionRethrows() {
        Executor rejecting = command -> {
            throw new RejectedExecutionException("test");
        };
        LightSubmit submit = new LightSubmit(new ChunkTaskScheduler(Runnable::run, rejecting, config()));

        assertThrows(RejectedExecutionException.class, () -> submit.accept(() -> fail("light work must not run")));
    }

    @Test
    void acceptedWorkRunsWithoutThrowing() {
        LightSubmit submit = new LightSubmit(new ChunkTaskScheduler(Runnable::run, Runnable::run, config()));
        AtomicBoolean ran = new AtomicBoolean();

        submit.accept(() -> ran.set(true));

        assertTrue(ran.get());
    }

    @Test
    void rejectedScheduleLeavesLaneIdleAndUsable() throws Exception {
        ExecutorService backing = Executors.newSingleThreadExecutor();
        AtomicBoolean rejectOnce = new AtomicBoolean(true);
        Executor flaky = command -> {
            if (rejectOnce.compareAndSet(true, false)) {
                throw new RejectedExecutionException("test");
            }
            backing.execute(command);
        };
        ChunkTaskScheduler tasks = new ChunkTaskScheduler(Runnable::run, flaky, config());
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        LightScheduler lane = new LightScheduler(view(chunks), new LightSubmit(tasks));
        try {
            lane.chunkLoaded(0, 0);
            assertTrue(lane.isIdle(), "rejected submission must park the lane");

            lane.chunkLoaded(0, 0);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!lane.isIdle() && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertTrue(lane.isIdle(), "lane did not drain");
            assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
        } finally {
            backing.shutdownNow();
            assertTrue(backing.awaitTermination(10, TimeUnit.SECONDS), "executor did not stop");
        }
    }

    private static GlobalConfiguration.ChunkGeneration config() {
        return GlobalConfiguration.ChunkGeneration.builder().generationConcurrency(1).build();
    }

    @Test
    void lightLaneDrainsThroughTheSharedSchedulerUnderGenerationLoad() throws Exception {
        GlobalConfiguration.ChunkGeneration generationConfig = GlobalConfiguration.ChunkGeneration.builder()
                .generationConcurrency(2).build();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        Executor generation = command -> pool.execute(() -> {
            try {
                gate.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            command.run();
        });
        ChunkTaskScheduler tasks = new ChunkTaskScheduler(Runnable::run, generation, generationConfig);
        try {
            Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT, Map.of("getSeed", 5L));
            // Occupy every generation slot, then queue more generation behind it.
            List<CompletableFuture<CloudChunk>> generationWork = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                generationWork.add(tasks.scheduleGeneration(100 + i, 0, generatedChunk(100 + i, 0, level),
                        ChunkTaskPriority.NORMAL));
            }

            Long2ObjectMap<CloudChunk> expected = LightTestChunks.overworld(5L, 0, 0, 3);
            Long2ObjectMap<CloudChunk> actual = LightTestChunks.overworld(5L, 0, 0, 3);
            LightScheduler lane = new LightScheduler(view(actual), new LightSubmit(tasks));
            for (long key : sortedKeys(actual)) {
                lane.chunkLoaded(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key));
                OracleLight.initialize(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, expected::get, expected.get(key));
            }

            gate.countDown();

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!lane.isIdle() && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertTrue(lane.isIdle(), "light backlog did not drain");
            LightTestChunks.assertSameLight(expected, actual);
            for (CompletableFuture<CloudChunk> future : generationWork) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "executor did not stop");
        }
    }

    private static CloudChunk generatedChunk(int chunkX, int chunkZ, Level level) {
        CloudChunk chunk = new CloudChunk(chunkX, chunkZ, level);
        chunk.advanceGenerationStatus(ChunkGenerationStatus.GENERATED);
        return chunk;
    }

    private static long[] sortedKeys(Long2ObjectMap<CloudChunk> chunks) {
        long[] keys = chunks.keySet().toLongArray();
        Arrays.sort(keys);
        return keys;
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
}
