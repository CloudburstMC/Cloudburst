package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class LightSchedulerTest {

    private final Fixture fixture = new Fixture();

    @AfterEach
    void closeFixture() {
        this.fixture.scheduler.close();

        for (CloudChunk chunk : this.fixture.ownedChunks) {
            chunk.close();
        }
    }

    @Test
    void completionRequiresEverySeedBandAndPropagationSlice() {
        fixture.createChunk(0);
        CloudChunk chunk = fixture.chunks.get(CloudChunk.key(0, 0));
        CompletableFuture<Void> ready = fixture.scheduler.chunkLoaded(0, 0);

        assertFalse(ready.isDone());
        assertEquals(0, chunk.getSkyLight(8, 15, 8));

        fixture.tasks.runNext();

        assertFalse(ready.isDone());

        fixture.tasks.drain(fixture.scheduler);

        ready.join();
        assertEquals(15, chunk.getSkyLight(8, 15, 8));
        assertTrue(fixture.scheduler.isIdle());
    }

    @Test
    void initializesNeighboursBeforePublishingReadyLight() {
        fixture.createChunk(0);
        fixture.createChunk(1);
        CloudChunk center = fixture.chunks.get(CloudChunk.key(0, 0));
        CloudChunk neighbour = fixture.chunks.get(CloudChunk.key(1, 0));
        neighbour.setBlockState(0, 8, 8, BlockLayer.PRIMARY, BlockTypes.GLOWSTONE.getDefaultState());
        CompletableFuture<Void> ready = fixture.scheduler.chunkLoaded(0, 0);

        fixture.tasks.drain(fixture.scheduler);

        ready.join();
        assertEquals(14, center.getBlockLight(15, 8, 8));

        fixture.scheduler.chunkLoaded(1, 0);
        fixture.tasks.runNext();

        assertEquals(14, center.getBlockLight(15, 8, 8));
        assertTrue(fixture.scheduler.isIdle());
    }

    @Test
    void unloadingClearsNeighbourLightAndForgetsInitialization() {
        fixture.createChunk(0);
        fixture.createChunk(1);
        CloudChunk source = fixture.chunks.get(CloudChunk.key(0, 0));
        CloudChunk neighbour = fixture.chunks.get(CloudChunk.key(1, 0));
        source.setBlockState(15, 8, 8, BlockLayer.PRIMARY, BlockTypes.GLOWSTONE.getDefaultState());
        fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.drain(fixture.scheduler);

        assertEquals(14, neighbour.getBlockLight(0, 8, 8));

        assertSame(source, fixture.chunks.remove(source.getKey()));
        fixture.scheduler.chunkUnloaded(0, 0);
        fixture.tasks.drain(fixture.scheduler);

        assertEquals(0, neighbour.getBlockLight(0, 8, 8));

        source.setBlockState(15, 8, 8, BlockLayer.PRIMARY, BlockStates.AIR);
        assertEquals(15, source.getBlockLight(15, 8, 8));
        fixture.chunks.put(source.getKey(), source);
        CompletableFuture<Void> reloaded = fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.drain(fixture.scheduler);

        reloaded.join();
        assertEquals(0, source.getBlockLight(15, 8, 8));
        assertEquals(0, neighbour.getBlockLight(0, 8, 8));
    }

    @Test
    void propagationRespectsTheSliceBudget() {
        fixture.createChunk(0);
        fixture.chunks.get(CloudChunk.key(0, 0))
                .setBlockState(8, 8, 8, BlockLayer.PRIMARY, BlockTypes.GLOWSTONE.getDefaultState());
        List<Integer> steps = new ArrayList<>();

        try (LightScheduler scheduler = new LightScheduler(fixture, fixture.tasks, 8,
                (_, count) -> steps.add(count))) {
            scheduler.chunkLoaded(0, 0);
            fixture.tasks.drain(scheduler);

            assertTrue(steps.contains(8));
            assertTrue(steps.stream().allMatch(count -> count >= 0 && count <= 8));
        }
    }

    @Test
    void unloadingBetweenSlicesCancelsReadiness() {
        fixture.createChunk(0);
        CompletableFuture<Void> ready = fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.runNext();
        assertNotNull(fixture.chunks.remove(CloudChunk.key(0, 0)));

        fixture.tasks.drain(fixture.scheduler);

        assertTrue(ready.isCompletedExceptionally());
        fixture.scheduler.checkFailure();
    }

    @Test
    void editsBetweenSlicesAreNotLost() {
        fixture.createChunk(0);
        CloudChunk chunk = fixture.chunks.get(CloudChunk.key(0, 0));
        fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.runNext();
        chunk.setBlockState(0, 8, 8, BlockLayer.PRIMARY, BlockTypes.GLOWSTONE.getDefaultState());
        CompletableFuture<Void> update = fixture.scheduler.blocksChanged(
                LongArrayList.of(LightPositions.pack(0, 8, 8)));

        fixture.tasks.drain(fixture.scheduler);

        update.join();
        assertEquals(15, chunk.getBlockLight(0, 8, 8));
        assertEquals(14, chunk.getBlockLight(1, 8, 8));

        chunk.setBlockState(0, 8, 8, BlockLayer.PRIMARY, BlockStates.AIR);
        fixture.scheduler.blocksChanged(LongArrayList.of(LightPositions.pack(0, 8, 8)));
        fixture.tasks.drain(fixture.scheduler);

        assertEquals(0, chunk.getBlockLight(1, 8, 8));
    }

    @Test
    void queuedBlockChangesAreDeduplicatedAndCancellationDoesNotDropWork() {
        fixture.createChunk(0);
        CloudChunk chunk = fixture.chunks.get(CloudChunk.key(0, 0));
        List<Integer> steps = new ArrayList<>();

        try (LightScheduler scheduler = new LightScheduler(fixture, fixture.tasks,
                LightScheduler.SLICE_STEPS, (_, count) -> steps.add(count))) {
            scheduler.chunkLoaded(0, 0);
            fixture.tasks.drain(scheduler);
            steps.clear();

            chunk.setBlockState(0, 8, 8, BlockLayer.PRIMARY, BlockTypes.GLOWSTONE.getDefaultState());
            LongArrayList positions = LongArrayList.of(LightPositions.pack(0, 8, 8));
            CompletableFuture<Void> cancelled = scheduler.blocksChanged(positions);
            List<CompletableFuture<Void>> updates = new ArrayList<>();

            for (int request = 0; request < 128; request++) {
                updates.add(scheduler.blocksChanged(positions));
            }

            cancelled.cancel(false);
            assertTrue(updates.stream().noneMatch(CompletableFuture::isDone));
            fixture.tasks.drain(scheduler);

            updates.forEach(CompletableFuture::join);
            assertEquals(15, chunk.getBlockLight(0, 8, 8));
            assertEquals(14, chunk.getBlockLight(1, 8, 8));
            assertEquals(1, steps.stream().filter(count -> count == 0).count(),
                    "Repeated pending changes should share one seed band");
        }
    }

    @Test
    void editsAfterAnUpdateStartsUseAnotherSeedPass() {
        fixture.createChunk(0);
        CloudChunk chunk = fixture.chunks.get(CloudChunk.key(0, 0));
        fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.drain(fixture.scheduler);

        chunk.setBlockState(0, 8, 8, BlockLayer.PRIMARY, BlockTypes.GLOWSTONE.getDefaultState());
        CompletableFuture<Void> first = fixture.scheduler.blocksChanged(
                LongArrayList.of(LightPositions.pack(0, 8, 8)));
        fixture.tasks.runNext();

        chunk.setBlockState(0, 8, 8, BlockLayer.PRIMARY, BlockStates.AIR);
        CompletableFuture<Void> second = fixture.scheduler.blocksChanged(
                LongArrayList.of(LightPositions.pack(0, 8, 8)));
        fixture.tasks.drain(fixture.scheduler);

        first.join();
        second.join();
        assertEquals(0, chunk.getBlockLight(0, 8, 8));
        assertEquals(0, chunk.getBlockLight(1, 8, 8));
    }

    @Test
    void blockUpdatesDoNotMergeAcrossAnUnload() {
        fixture.createChunk(0);
        fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.drain(fixture.scheduler);

        LongArrayList positions = LongArrayList.of(LightPositions.pack(0, 8, 8));
        CompletableFuture<Void> before = fixture.scheduler.blocksChanged(positions);
        fixture.scheduler.chunkUnloaded(1, 0);
        CompletableFuture<Void> after = fixture.scheduler.blocksChanged(positions);

        for (int slice = 0; slice < 100 && !before.isDone(); slice++) {
            fixture.tasks.runNext();
        }

        assertTrue(before.isDone());
        before.join();
        assertFalse(after.isDone(), "An unload must remain between the two block updates");

        fixture.tasks.drain(fixture.scheduler);
        after.join();
    }

    @Test
    void busyNeighbourDoesNotRetainEarlierChunkLocks() throws Exception {
        fixture.createChunk(0);
        fixture.createChunk(1);
        CloudChunk earlier = fixture.chunks.get(CloudChunk.key(0, 0));
        CloudChunk busy = fixture.chunks.get(CloudChunk.key(1, 0));
        CompletableFuture<Void> ready = fixture.scheduler.chunkLoaded(0, 0);

        try (LockedChunk _ = busy.lockForWrite()) {
            CompletableFuture.runAsync(fixture.tasks::runNext).get(2, TimeUnit.SECONDS);

            assertFalse(ready.isDone());

            try (LockedChunk available = earlier.tryLockForWrite()) {
                assertNotNull(available, "A busy neighbour must not leave the earlier chunk locked");
            }
        }

        fixture.tasks.drain(fixture.scheduler);
        ready.join();
    }

    @Test
    void closeCancelsQueuedAndPartiallyExecutedRequests() {
        fixture.createChunk(0);
        CloudChunk chunk = fixture.chunks.get(CloudChunk.key(0, 0));
        CompletableFuture<Void> first = fixture.scheduler.chunkLoaded(0, 0);
        CompletableFuture<Void> second = fixture.scheduler.chunkLoaded(0, 0);
        fixture.tasks.runNext();

        fixture.scheduler.close();
        fixture.tasks.runNext();

        assertTrue(first.isCompletedExceptionally());
        assertTrue(second.isCompletedExceptionally());
        assertTrue(fixture.scheduler.chunkLoaded(0, 0).isCompletedExceptionally());
        assertTrue(fixture.scheduler.isIdle());
        assertEquals(0, chunk.getSkyLight(15, 15, 15));
        fixture.scheduler.checkFailure();
    }

    @Test
    void delayedSubmissionFailuresFailReadinessAndStopTheLane() {
        fixture.createChunk(0);
        CompletableFuture<Void> accepted = new CompletableFuture<>();

        try (LightScheduler scheduler = new LightScheduler(fixture, _ -> accepted)) {
            CompletableFuture<Void> ready = scheduler.chunkLoaded(0, 0);

            accepted.completeExceptionally(new RejectedExecutionException("Worker stopped"));

            assertThrows(CompletionException.class, ready::join);
            assertThrows(CompletionException.class, scheduler::checkFailure);
            assertTrue(scheduler.isIdle());
            assertTrue(scheduler.chunkLoaded(0, 0).isCompletedExceptionally());
        }
    }

    @Test
    void sliceFailureReleasesLocksAndFailsOutstandingRequests() throws Exception {
        fixture.createChunk(0);
        CloudChunk chunk = fixture.chunks.get(CloudChunk.key(0, 0));

        try (LightScheduler scheduler = new LightScheduler(fixture, fixture.tasks, 8, (_, _) -> {
            throw new IllegalStateException("Failed light slice");
        })) {
            CompletableFuture<Void> first = scheduler.chunkLoaded(0, 0);
            CompletableFuture<Void> second = scheduler.chunkLoaded(0, 0);

            fixture.tasks.runNext();

            assertTrue(first.isCompletedExceptionally());
            assertTrue(second.isCompletedExceptionally());
            assertThrows(CompletionException.class, scheduler::checkFailure);
            boolean released = CompletableFuture.supplyAsync(() -> {
                try (LockedChunk locked = chunk.tryLockForWrite()) {
                    return locked != null;
                }
            }).get(2, TimeUnit.SECONDS);
            assertTrue(released);
        }
    }

    @Test
    void unloadedRequestsAreCancelledWithoutFailingOtherChunks() {
        fixture.createChunk(1);
        CompletableFuture<Void> missing = fixture.scheduler.chunkLoaded(0, 0);
        CompletableFuture<Void> present = fixture.scheduler.chunkLoaded(1, 0);

        fixture.tasks.drain(fixture.scheduler);

        assertTrue(missing.isCompletedExceptionally());
        present.join();
        fixture.scheduler.checkFailure();
    }

    private static class Fixture implements LightLevelView {

        private final BlockRegistry registry = BlockTestRegistry.get();
        private final Level level = TestLevels.level(0, 16);
        private final Map<Long, CloudChunk> chunks = new ConcurrentHashMap<>();
        private final List<CloudChunk> ownedChunks = new ArrayList<>();
        private final LightTestTasks tasks = new LightTestTasks();
        private final LightScheduler scheduler = new LightScheduler(this, this.tasks);

        private void createChunk(int x) {
            CloudChunk chunk = new CloudChunk(x, 0, this.level);
            this.ownedChunks.add(chunk);
            this.chunks.put(chunk.getKey(), chunk);
        }

        @Override
        public BlockRegistry registry() {
            return this.registry;
        }

        @Override
        public int minHeight() {
            return 0;
        }

        @Override
        public int maxHeight() {
            return 16;
        }

        @Override
        public boolean hasSkyLight() {
            return true;
        }

        @Override
        public CloudChunk getChunkForLighting(int chunkX, int chunkZ) {
            return this.chunks.get(CloudChunk.key(chunkX, chunkZ));
        }
    }
}
