package org.cloudburstmc.server.diagnostics.spark;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SparkMainThreadExecutorTest {

    @Test
    void completesServerThreadPollingWithoutWaitingForAnotherTick() {
        Thread owner = Thread.currentThread();
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(owner)) {
            CompletableFuture<Thread> result = CompletableFuture.supplyAsync(Thread::currentThread, executor);

            assertTrue(result.isDone(), "Polling on the server thread must not wait for another tick");
            assertEquals(owner, result.join());
        }
    }

    @Test
    void queuesBackgroundPollingUntilTheServerDrainsIt() {
        Thread owner = Thread.currentThread();
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(owner)) {
            CompletableFuture<Thread> result = CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(Thread::currentThread, executor)).join();

            assertFalse(result.isDone());
            executor.drain();

            assertEquals(owner, result.join());
        }
    }

    @Test
    void runsNestedServerThreadTasksInline() {
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread())) {
            List<Integer> order = new ArrayList<>();
            executor.execute(() -> {
                order.add(1);
                executor.execute(() -> order.add(2));
                order.add(3);
            });

            assertEquals(List.of(1, 2, 3), order);
        }
    }

    @Test
    void finishesAcceptedPollingBeforeClosingAndRejectsNewWork() {
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread())) {
            CompletableFuture<Integer> result = CompletableFuture.supplyAsync(() -> CompletableFuture.supplyAsync(() -> 42, executor)).join();

            executor.close();

            assertTrue(result.isDone());
            assertEquals(Integer.valueOf(42), result.join());
            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {}));
        }
    }

    @Test
    void rejectsDrainingFromAnotherThread() {
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread())) {
            CompletionException failure = assertThrows(CompletionException.class, () -> CompletableFuture.runAsync(executor::drain).join());

            assertInstanceOf(IllegalStateException.class, failure.getCause());
        }
    }

    @Test
    void rejectsClosingFromAnotherThreadWithoutClosingTheQueue() {
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread())) {
            CompletionException failure = assertThrows(CompletionException.class, () -> CompletableFuture.runAsync(executor::close).join());

            assertInstanceOf(IllegalStateException.class, failure.getCause());
            CompletableFuture<Integer> result = CompletableFuture.supplyAsync(() -> 42, executor);
            assertEquals(Integer.valueOf(42), result.join());
        }
    }

    @Test
    void emergencyShutdownDoesNotExecuteQueuedWorkOffThread() {
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread())) {
            AtomicBoolean executed = new AtomicBoolean();
            CompletableFuture.runAsync(() -> executor.execute(() -> executed.set(true))).join();

            CompletableFuture.runAsync(executor::shutdownNow).join();
            executor.drain();

            assertFalse(executed.get());
            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {}));
        }
    }
}
