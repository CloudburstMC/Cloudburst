package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.server.level.DimensionHeight;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudLevelLightViewTest {

    private static final BlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final int MIN_HEIGHT = DimensionHeight.OVERWORLD.getMinHeight();
    private static final int MAX_HEIGHT = DimensionHeight.OVERWORLD.getMaxHeight();

    @Test
    void preservesTheSkyLightSetting() {
        assertTrue(view(true).hasSkyLight());
        assertFalse(view(false).hasSkyLight());
    }

    @Test
    void preservesTheRegistryAndHeightLimits() {
        LightLevelView view = view(true);

        assertSame(REGISTRY, view.registry());
        assertEquals(MIN_HEIGHT, view.minHeight());
        assertEquals(MAX_HEIGHT, view.maxHeight());
    }

    @Test
    void resolvesCurrentChunksThroughTheSuppliedLookup() {
        try (CloudChunk chunk = new CloudChunk(-3, 5, TestLevels.level(MIN_HEIGHT, MAX_HEIGHT))) {
            Long2ObjectMap<CloudChunk> chunks = new Long2ObjectOpenHashMap<>();
            chunks.put(chunk.getKey(), chunk);
            LightLevelView view = new CloudLevelLightView(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, true, chunks::get);

            assertSame(chunk, view.getChunkForLighting(-3, 5));
            assertNull(view.getChunkForLighting(0, 0));

            chunks.keySet().remove(chunk.getKey());

            assertNull(view.getChunkForLighting(-3, 5));
        }
    }

    private static LightLevelView view(boolean skyLight) {
        return new CloudLevelLightView(REGISTRY, MIN_HEIGHT, MAX_HEIGHT, skyLight, key -> null);
    }
}
