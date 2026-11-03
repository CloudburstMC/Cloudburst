package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.*;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightPositionsTest {

    @Test
    void packingRoundTripsNegativeAndExtremeCoordinates() {
        int[] horizontal = {-30_000_000, -1, 0, 15, 16, 29_999_999};
        int[] vertical = {-64, -1, 0, 319};

        for (int x : horizontal) {
            for (int z : horizontal) {
                for (int y : vertical) {
                    long packed = LightPositions.pack(x, y, z);
                    assertEquals(x, LightPositions.x(packed), () -> "x of " + x + "," + y + "," + z);
                    assertEquals(y, LightPositions.y(packed), () -> "y of " + x + "," + y + "," + z);
                    assertEquals(z, LightPositions.z(packed), () -> "z of " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void queuedChangesIgnoreTheBlockLayer() {
        int minHeight = -64;
        long chunkKey = CloudChunk.key(-3, 5);
        int primary = CloudChunk.blockKeyWithLayer(1, 64, 2, BlockLayer.PRIMARY, minHeight);
        int secondary = CloudChunk.blockKeyWithLayer(1, 64, 2, BlockLayer.SECONDARY, minHeight);
        int other = CloudChunk.blockKeyWithLayer(15, minHeight, 14, BlockLayer.PRIMARY, minHeight);
        Long2ObjectMap<IntSet> queue = new Long2ObjectOpenHashMap<>();
        IntSet keys = new IntOpenHashSet();
        keys.add(primary);
        keys.add(secondary);
        keys.add(other);
        queue.put(chunkKey, keys);

        LongArrayList collected = LightPositions.collectChanges(queue, minHeight);

        LongSet packed = new LongOpenHashSet(collected);
        assertEquals(2, packed.size());
        assertTrue(packed.contains(CloudChunk.lightPosition(chunkKey, primary, minHeight)));
        assertTrue(packed.contains(CloudChunk.lightPosition(chunkKey, other, minHeight)));

        assertTrue(LightPositions.collectChanges(new Long2ObjectOpenHashMap<>(), minHeight).isEmpty());
    }
}
