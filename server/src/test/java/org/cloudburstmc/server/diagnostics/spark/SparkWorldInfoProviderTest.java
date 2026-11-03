package org.cloudburstmc.server.diagnostics.spark;

import me.lucko.spark.common.platform.world.ChunkInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SparkWorldInfoProviderTest {

    @Test
    void backgroundSamplingRetainsPublishedCountsAfterShutdown() {
        Source source = new Source();
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread());
             SparkWorldInfoProvider provider = new SparkWorldInfoProvider(source, executor)) {
            provider.refresh();
            WorldInfoProvider.CountsResult counts = provider.pollCounts();

            assertFalse(provider.mustCallSync());
            assertEquals(2, counts.players());
            assertEquals(3, counts.entities());
            assertEquals(4, counts.tileEntities());
            assertEquals(5, counts.chunks());

            provider.close();
            executor.close();

            assertSame(counts, CompletableFuture.supplyAsync(provider::pollCounts).join());
            assertNull(CompletableFuture.supplyAsync(provider::pollChunks).join());
            assertNull(CompletableFuture.supplyAsync(provider::pollGameRules).join());
            assertNull(CompletableFuture.supplyAsync(provider::pollDataPacks).join());
            assertEquals(1, source.countCalls.get());
            assertEquals(0, source.detailCalls.get());
        }
    }

    @Test
    void rejectsCapturingLiveCountsFromBackgroundThreads() {
        Source source = new Source();
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread());
             SparkWorldInfoProvider provider = new SparkWorldInfoProvider(source, executor)) {
            CompletionException failure = assertThrows(CompletionException.class,
                    () -> CompletableFuture.runAsync(provider::refresh).join());

            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(0, source.countCalls.get());
        }
    }

    @Test
    void runsBackgroundDetailRequestsOnTheServerThread() throws InterruptedException {
        CountDownLatch queued = new CountDownLatch(1);
        Source source = new Source();
        Thread owner = Thread.currentThread();
        try (SparkMainThreadExecutor executor = signallingExecutor(queued);
             SparkWorldInfoProvider provider = new SparkWorldInfoProvider(source, executor)) {
            CompletableFuture<WorldInfoProvider.GameRulesResult> result =
                    CompletableFuture.supplyAsync(provider::pollGameRules);

            assertTrue(queued.await(2, TimeUnit.SECONDS));
            assertFalse(result.isDone());

            executor.drain();

            assertNotNull(result.join());
            assertSame(owner, source.detailThread.get());
            assertEquals(1, source.detailCalls.get());
        }
    }

    @Test
    void shutdownCompletesQueuedQueriesWithoutAccessingLiveWorlds() throws InterruptedException {
        CountDownLatch queued = new CountDownLatch(1);
        Source source = new Source();
        try (SparkMainThreadExecutor executor = signallingExecutor(queued);
             SparkWorldInfoProvider provider = new SparkWorldInfoProvider(source, executor)) {
            CompletableFuture<WorldInfoProvider.GameRulesResult> result =
                    CompletableFuture.supplyAsync(provider::pollGameRules);

            assertTrue(queued.await(2, TimeUnit.SECONDS));

            provider.close();
            executor.close();

            assertNull(result.join());
            assertEquals(0, source.detailCalls.get());
        }
    }

    @Test
    void doesNotHideExecutorFailuresWhileMonitoringIsOpen() {
        Source source = new Source();
        try (SparkMainThreadExecutor executor = new SparkMainThreadExecutor(Thread.currentThread());
             SparkWorldInfoProvider provider = new SparkWorldInfoProvider(source, executor)) {
            executor.close();

            assertThrows(java.util.concurrent.RejectedExecutionException.class, provider::pollGameRules);
        }
    }

    private static SparkMainThreadExecutor signallingExecutor(CountDownLatch queued) {
        return new SparkMainThreadExecutor(Thread.currentThread()) {
            @Override
            public void execute(@NonNull Runnable task) {
                super.execute(task);
                queued.countDown();
            }
        };
    }

    private static class Source implements WorldInfoProvider {

        private final AtomicInteger countCalls = new AtomicInteger();
        private final AtomicInteger detailCalls = new AtomicInteger();
        private final AtomicReference<Thread> detailThread = new AtomicReference<>();

        @Override
        public CountsResult pollCounts() {
            this.countCalls.incrementAndGet();
            return new CountsResult(2, 3, 4, 5);
        }

        @Override
        public ChunksResult<? extends ChunkInfo<?>> pollChunks() {
            throw new AssertionError("Unexpected chunk query");
        }

        @Override
        public GameRulesResult pollGameRules() {
            this.detailCalls.incrementAndGet();
            this.detailThread.set(Thread.currentThread());
            return new GameRulesResult();
        }

        @Override
        public Collection<DataPackInfo> pollDataPacks() {
            throw new AssertionError("Unexpected data-pack query");
        }
    }
}
