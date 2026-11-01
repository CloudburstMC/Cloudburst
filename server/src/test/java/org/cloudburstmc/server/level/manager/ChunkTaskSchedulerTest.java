package org.cloudburstmc.server.level.manager;

import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ChunkTaskSchedulerTest {

    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void lightRunsBeforeQueuedGeneration() throws Exception {
        GlobalConfiguration.ChunkGeneration config = GlobalConfiguration.ChunkGeneration.builder()
                .generationConcurrency(1)
                .build();
        ExecutorService backing = Executors.newSingleThreadExecutor();
        CountDownLatch gate = new CountDownLatch(1);
        Executor generation = command -> backing.execute(() -> {
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            command.run();
        });
        ChunkTaskScheduler scheduler = new ChunkTaskScheduler(Runnable::run, generation, config);
        try {
            Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT, Map.of("getSeed", 5L));
            List<String> order = Collections.synchronizedList(new ArrayList<>());
            CompletableFuture<CloudChunk> first =
                    scheduler.scheduleGeneration(0, 0, generatedChunk(0, 0, level), ChunkTaskPriority.NORMAL);
            CompletableFuture<CloudChunk> second =
                    scheduler.scheduleGeneration(1, 0, generatedChunk(1, 0, level), ChunkTaskPriority.NORMAL);
            CompletableFuture<Void> generationRecorded =
                    second.thenRun(() -> order.add("generation"));
            CompletableFuture<Void> light = scheduler.scheduleLight(() -> order.add("light"));

            gate.countDown();

            light.get(30, TimeUnit.SECONDS);
            first.get(30, TimeUnit.SECONDS);
            generationRecorded.get(30, TimeUnit.SECONDS);
            assertEquals(List.of("light", "generation"), order);
        } finally {
            backing.shutdownNow();
            assertTrue(backing.awaitTermination(10, TimeUnit.SECONDS), "generation executor did not stop");
        }
    }

    @Test
    void lightReservesNoArea() throws Exception {
        GlobalConfiguration.ChunkGeneration config = GlobalConfiguration.ChunkGeneration.builder()
                .generationConcurrency(2)
                .build();
        ExecutorService generation = Executors.newFixedThreadPool(2);
        ChunkTaskScheduler scheduler = new ChunkTaskScheduler(Runnable::run, generation, config);
        try {
            Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT, Map.of("getSeed", 5L));
            CloudChunk center = generatedChunk(0, 0, level);
            CloudChunk neighbor = generatedChunk(1, 0, level);
            CompletableFuture<CloudChunk> populate;
            try (LockedChunk held = neighbor.lockForWrite()) {
                populate = scheduler.schedulePopulation(0, 0, center, List.of(neighbor), ChunkTaskPriority.NORMAL);
                CompletableFuture<Void> light = scheduler.scheduleLight(() -> {
                });

                light.get(30, TimeUnit.SECONDS);
                assertFalse(populate.isDone(), "populate must still be blocked on the neighbor lock");
            }
            // Unblocked, the populate task runs to completion; any outcome proves
            // the worker was genuinely inside the task.
            try {
                populate.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException expected) {
                // The stub level cannot populate: reaching this means the task
                // ran instead of waiting on the reserved area.
            }
        } finally {
            generation.shutdownNow();
            assertTrue(generation.awaitTermination(10, TimeUnit.SECONDS), "generation executor did not stop");
        }
    }

    @Test
    void rejectedLightScheduleCompletesTheFutureExceptionally() {
        Executor rejecting = command -> {
            throw new RejectedExecutionException("test");
        };
        ChunkTaskScheduler scheduler = new ChunkTaskScheduler(Runnable::run, rejecting,
                GlobalConfiguration.ChunkGeneration.builder().generationConcurrency(1).build());

        CompletableFuture<Void> future = scheduler.scheduleLight(() -> fail("light work must not run"));

        // The CloudLevel adapter relies on this: a synchronous rejection
        // surfaces on the future so it can rethrow it to the light lane.
        assertTrue(future.isCompletedExceptionally());
        assertTrue(future.exceptionNow() instanceof RejectedExecutionException);
    }

    private static CloudChunk generatedChunk(int chunkX, int chunkZ, Level level) {
        CloudChunk chunk = new CloudChunk(chunkX, chunkZ, level);
        chunk.advanceGenerationStatus(ChunkGenerationStatus.GENERATED);
        return chunk;
    }
}
