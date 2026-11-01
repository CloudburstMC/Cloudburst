package org.cloudburstmc.server.level;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.light.LightLevelView;
import org.cloudburstmc.server.level.light.LightScheduler;
import org.cloudburstmc.server.level.light.LightTestChunks;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Covers the CloudLevel light wiring without a level: the background lane's
 * view, the tick drain into packed positions, and the enqueue-only promotion,
 * unload and tick calls.
 */
class CloudLevelLightWiringTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void lightViewHasSkyLightOnlyInOverworld() {
        assertTrue(view(CloudLevel.DIMENSION_OVERWORLD).hasSkyLight());
        assertFalse(view(CloudLevel.DIMENSION_NETHER).hasSkyLight());
        assertFalse(view(CloudLevel.DIMENSION_THE_END).hasSkyLight());
    }

    @Test
    void lightViewPassesThroughRegistryAndHeights() {
        LightLevelView view = view(CloudLevel.DIMENSION_OVERWORLD);
        assertSame(REGISTRY, view.registry());
        assertEquals(MIN_HEIGHT, view.minHeight());
        assertEquals(MAX_HEIGHT, view.maxHeight());
    }

    @Test
    void lightViewExposesOnlyLoadedChunks() {
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        long key = CloudChunk.key(0, 0);
        LightLevelView view = new CloudLevel.LightView(REGISTRY, MIN_HEIGHT, MAX_HEIGHT,
                CloudLevel.DIMENSION_OVERWORLD, chunkKey -> chunks.get(chunkKey));

        // The production view delegates to LevelChunkManager.getLoadedChunk,
        // which returns only complete promoted chunks; the view surfaces
        // whatever the loader holds and null when absent.
        assertSame(chunks.get(key), view.loadedChunk(0, 0));
        assertNull(view.loadedChunk(1, 0));
    }

    @Test
    void tickDrainPacksPositionsThroughLightPosition() {
        long chunkKey = CloudChunk.key(-3, 5);
        int primary = CloudChunk.blockKeyWithLayer(1, 64, 2, BlockLayer.PRIMARY, MIN_HEIGHT);
        int secondary = CloudChunk.blockKeyWithLayer(1, 64, 2, BlockLayer.SECONDARY, MIN_HEIGHT);
        int other = CloudChunk.blockKeyWithLayer(15, MIN_HEIGHT, 14, BlockLayer.PRIMARY, MIN_HEIGHT);
        Long2ObjectMap<IntSet> queue = new Long2ObjectOpenHashMap<>();
        IntSet keys = new IntOpenHashSet();
        keys.add(primary);
        keys.add(secondary);
        keys.add(other);
        queue.put(chunkKey, keys);

        LongArrayList collected = CloudLevel.collectLightPositions(queue, MIN_HEIGHT);

        // The layer bit is dropped: the primary and secondary entries for one
        // block collapse into a single packed position.
        LongSet packed = new LongOpenHashSet(collected);
        assertEquals(2, packed.size());
        assertTrue(packed.contains(CloudChunk.lightPosition(chunkKey, primary, MIN_HEIGHT)));
        assertTrue(packed.contains(CloudChunk.lightPosition(chunkKey, other, MIN_HEIGHT)));

        assertTrue(CloudLevel.collectLightPositions(new Long2ObjectOpenHashMap<>(), MIN_HEIGHT).isEmpty());
    }

    @Test
    void promotionUnloadAndTickOnlyEnqueue() {
        // The same scheduler calls CloudLevel.initializeChunkLight,
        // updateChunkLightBoundary and doTick's updateQueuedLight make.
        Long2ObjectMap<CloudChunk> chunks = LightTestChunks.overworld(5L, 0, 0, 1);
        List<Runnable> pending = new ArrayList<>();
        LightLevelView view = new CloudLevel.LightView(REGISTRY, MIN_HEIGHT, MAX_HEIGHT,
                CloudLevel.DIMENSION_OVERWORLD, chunks::get);
        LightScheduler lane = new LightScheduler(view, pending::add);

        lane.chunkLoaded(0, 0);
        lane.chunkUnloaded(0, 0);
        Long2ObjectMap<IntSet> queue = new Long2ObjectOpenHashMap<>();
        IntSet keys = new IntOpenHashSet();
        keys.add(CloudChunk.blockKeyWithLayer(8, 70, 8, BlockLayer.PRIMARY, MIN_HEIGHT));
        queue.put(CloudChunk.key(0, 0), keys);
        lane.blocksChanged(CloudLevel.collectLightPositions(queue, MIN_HEIGHT));

        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 0);
        assertEquals(1, pending.size());

        drain(lane, pending);
        assertTrue(lane.isIdle());
        assertSkyTop(chunks.get(CloudChunk.key(0, 0)), 15);
    }

    private static LightLevelView view(int dimension) {
        return new CloudLevel.LightView(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, dimension, key -> null);
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
