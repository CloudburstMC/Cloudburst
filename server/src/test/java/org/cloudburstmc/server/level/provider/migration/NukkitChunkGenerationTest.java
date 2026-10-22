package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NukkitChunkGenerationTest {

    @Test
    void populatedLevelDBTerrainDoesNotRunCloudburstFinishers() throws IOException {
        assertEquals(ChunkGenerationStatus.FINISHED, NukkitChunkGeneration.readLevelDB(finalizationState(1)));
        assertEquals(ChunkGenerationStatus.FINISHED, NukkitChunkGeneration.readLevelDB(finalizationState(2)));
        assertEquals(ChunkGenerationStatus.FINISHED, NukkitChunkGeneration.readLevelDB(null));
    }

    @Test
    void populatedAnvilTerrainDoesNotRunCloudburstFinishers() {
        for (boolean generated : new boolean[]{false, true}) {
            NbtMap tag = NbtMap.builder()
                    .putBoolean("TerrainGenerated", generated)
                    .putBoolean("TerrainPopulated", true)
                    .build();

            assertEquals(ChunkGenerationStatus.FINISHED, NukkitChunkGeneration.readAnvil(tag));
        }
    }

    @Test
    void incompleteTerrainRetainsItsGenerationProgress() throws IOException {
        assertEquals(ChunkGenerationStatus.NEW, NukkitChunkGeneration.readLevelDB(finalizationState(-1)));
        assertEquals(ChunkGenerationStatus.GENERATED, NukkitChunkGeneration.readLevelDB(finalizationState(0)));
        assertEquals(ChunkGenerationStatus.NEW, NukkitChunkGeneration.readAnvil(NbtMap.EMPTY));
        assertEquals(ChunkGenerationStatus.GENERATED, NukkitChunkGeneration.readAnvil(
                NbtMap.builder().putBoolean("TerrainGenerated", true).build()));
    }

    @Test
    void malformedFinalizationStatesAreRejected() {
        assertThrows(IOException.class, () -> NukkitChunkGeneration.readLevelDB(new byte[1]));
        assertThrows(IOException.class, () -> NukkitChunkGeneration.readLevelDB(finalizationState(-2)));
        assertThrows(IOException.class, () -> NukkitChunkGeneration.readLevelDB(finalizationState(3)));
    }

    private static byte[] finalizationState(int value) {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
}
