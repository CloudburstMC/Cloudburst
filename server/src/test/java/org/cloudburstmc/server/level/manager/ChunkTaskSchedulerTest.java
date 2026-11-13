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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ChunkTaskSchedulerTest {

    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void lightRunsBeforeQueuedGeneration() throws Exception {
        GlobalConfiguration.ChunkGeneration config = GlobalConfiguration.ChunkGeneration.builder()
                .generationConcurrency(1)
                .build();
        try (ExecutorService backing = Executors.newSingleThreadExecutor()) {
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
            Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT, Map.of("getSeed", 5L));

            try (CloudChunk firstChunk = generatedChunk(0, level); CloudChunk secondChunk = generatedChunk(1, level)) {
                List<String> order = Collections.synchronizedList(new ArrayList<>());
                CompletableFuture<CloudChunk> first = scheduler.scheduleGeneration(0, 0, firstChunk, ChunkTaskPriority.NORMAL);
                CompletableFuture<CloudChunk> second = scheduler.scheduleGeneration(1, 0, secondChunk, ChunkTaskPriority.NORMAL);
                CompletableFuture<Void> generationRecorded = second.thenRun(() -> order.add("generation"));
                CompletableFuture<Void> light = scheduler.scheduleLight(() -> order.add("light"));

                gate.countDown();

                light.get(30, TimeUnit.SECONDS);
                first.get(30, TimeUnit.SECONDS);
                generationRecorded.get(30, TimeUnit.SECONDS);
                assertEquals(List.of("light", "generation"), order);
            } finally {
                gate.countDown();
                backing.shutdownNow();
                assertTrue(backing.awaitTermination(10, TimeUnit.SECONDS), "generation executor did not stop");
            }
        }
    }

    @Test
    void lightReservesNoArea() throws Exception {
        GlobalConfiguration.ChunkGeneration config = GlobalConfiguration.ChunkGeneration.builder()
                .generationConcurrency(2)
                .build();
        try (ExecutorService generation = Executors.newFixedThreadPool(2)) {
            ChunkTaskScheduler scheduler = new ChunkTaskScheduler(Runnable::run, generation, config);
            Level level = TestLevels.level(MIN_HEIGHT, MAX_HEIGHT, Map.of("getSeed", 5L));

            try (CloudChunk center = generatedChunk(0, level);
                 CloudChunk neighbor = generatedChunk(1, level)) {
                CompletableFuture<CloudChunk> populate;

                try (LockedChunk _ = neighbor.lockForWrite()) {
                    populate = scheduler.schedulePopulation(0, 0, center, List.of(neighbor), ChunkTaskPriority.NORMAL);
                    CompletableFuture<Void> light = scheduler.scheduleLight(() -> {
                    });

                    light.get(30, TimeUnit.SECONDS);
                    assertFalse(populate.isDone(), "populate must still be blocked on the neighbor lock");
                }

                // Either outcome proves the population task stopped waiting for the lock.
                try {
                    populate.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException expected) {
                    // The stub level cannot populate.
                }
            } finally {
                generation.shutdownNow();
                assertTrue(generation.awaitTermination(10, TimeUnit.SECONDS), "generation executor did not stop");
            }
        }
    }

    @Test
    void rejectingAQueuedBacklogFailsEveryFutureWithoutRecursion() {
        List<Runnable> accepted = new ArrayList<>(1);
        Executor executor = command -> {
            if (accepted.isEmpty()) {
                accepted.add(command);
            } else {
                throw new RejectedExecutionException("Worker stopped");
            }
        };

        ChunkTaskScheduler scheduler = new ChunkTaskScheduler(Runnable::run, executor, GlobalConfiguration.ChunkGeneration.builder().generationConcurrency(1).build());
        CompletableFuture<Void> first = scheduler.scheduleLight(() -> {});
        List<CompletableFuture<Void>> pending = new ArrayList<>();

        for (int request = 0; request < 65_536; request++) {
            pending.add(scheduler.scheduleLight(() -> fail("Rejected work must not run")));
        }

        assertFalse(first.isDone());
        assertDoesNotThrow(accepted.getFirst()::run);
        first.join();

        for (CompletableFuture<Void> future : pending) {
            assertTrue(future.isCompletedExceptionally());
            assertInstanceOf(RejectedExecutionException.class, future.exceptionNow());
        }
    }

    @Test
    void rejectedLightScheduleCompletesTheFutureExceptionally() {
        Executor rejecting = _ -> {
            throw new RejectedExecutionException("test");
        };
        ChunkTaskScheduler scheduler = new ChunkTaskScheduler(Runnable::run, rejecting,
                GlobalConfiguration.ChunkGeneration.builder().generationConcurrency(1).build());

        CompletableFuture<Void> future = scheduler.scheduleLight(() -> fail("light work must not run"));

        // The CloudLevel adapter relies on this: a synchronous rejection
        // surfaces on the future so it can rethrow it to the light lane.
        assertTrue(future.isCompletedExceptionally());
        assertInstanceOf(RejectedExecutionException.class, future.exceptionNow());
    }

    private static CloudChunk generatedChunk(int chunkX, Level level) {
        CloudChunk chunk = new CloudChunk(chunkX, 0, level);
        chunk.advanceGenerationStatus(ChunkGenerationStatus.GENERATED);
        return chunk;
    }
}
