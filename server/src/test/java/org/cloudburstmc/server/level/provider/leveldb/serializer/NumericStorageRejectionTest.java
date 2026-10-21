package org.cloudburstmc.server.level.provider.leveldb.serializer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NumericStorageRejectionTest {

    @Test
    void rejectsNumericChunkFormatsBeforeAccessingStorage() {
        for (int version : new int[]{0, 1, 2}) {
            assertThrows(UnsupportedOperationException.class, () -> ChunkSerializers.deserializeChunk(null, null, version));
        }
    }

    @Test
    void rejectsNumericSectionFormatsBeforeReadingBlocks() {
        for (int version : new int[]{0, 2, 3, 4, 5, 6, 7}) {
            assertThrows(UnsupportedOperationException.class, () -> ChunkSectionSerializers.deserialize(null, null, version));
        }
    }
}
