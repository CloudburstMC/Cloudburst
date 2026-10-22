package org.cloudburstmc.server.level.provider.migration;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBImportKeysTest {

    @Test
    void preservesCoordinatesDimensionsAndSignedSectionY() {
        for (int dimension : new int[]{0, 1, 2}) {
            byte[] key = LevelDBImportKeys.key(-123456, 654321, dimension, ',');
            byte[] section = LevelDBImportKeys.section(-123456, 654321, dimension, -4);
            LevelDBImportKeys.ChunkAddress expected = new LevelDBImportKeys.ChunkAddress(-123456, 654321, dimension);

            assertEquals(dimension == 0 ? 9 : 13, key.length);
            assertEquals(expected, LevelDBImportKeys.storedChunk(key).orElseThrow());
            assertEquals(expected, LevelDBImportKeys.storedChunk(LevelDBImportKeys.key(-123456, 654321, dimension, 'v')).orElseThrow());
            assertEquals(-4, section[section.length - 1]);
        }
    }

    @Test
    void ignoresNonChunkKeysAndMalformedSuffixes() {
        assertTrue(LevelDBImportKeys.storedChunk(new byte[0]).isEmpty());
        for (int dimension : new int[]{0, 1, 2}) {
            for (int tag : new int[]{'/', '?', '0', '1', '2'}) {
                assertTrue(LevelDBImportKeys.storedChunk(LevelDBImportKeys.key(1, 2, dimension, tag)).isEmpty());
            }

            assertTrue(LevelDBImportKeys.storedChunk(LevelDBImportKeys.section(1, 2, dimension, -4)).isEmpty());
            assertTrue(LevelDBImportKeys.storedChunk(LevelDBImportKeys.actorIndex(1, 2, dimension)).isEmpty());
        }
    }

    @Test
    void encodesModernActorKeysWithDimensionQualifiedIndexes() {
        byte[] storageKey = HexFormat.of().parseHex("0000000200000002");
        byte[] key = LevelDBImportKeys.actor(storageKey);
        assertEquals("actorprefix", new String(key, 0, 11, StandardCharsets.US_ASCII));
        assertArrayEquals(storageKey, Arrays.copyOfRange(key, 11, key.length));
        assertThrows(IllegalArgumentException.class, () -> LevelDBImportKeys.actor(new byte[7]));
        assertThrows(IllegalArgumentException.class, () -> LevelDBImportKeys.actor(new byte[9]));

        for (int dimension : new int[]{0, 1, 2}) {
            byte[] index = LevelDBImportKeys.actorIndex(-3, 7, dimension);
            ByteBuffer data = ByteBuffer.wrap(index).order(ByteOrder.LITTLE_ENDIAN);

            assertEquals("digp", new String(index, 0, 4, StandardCharsets.US_ASCII));
            assertEquals(-3, data.getInt(4));
            assertEquals(7, data.getInt(8));
            assertEquals(dimension == 0 ? 12 : 16, index.length);
            if (dimension != 0) {
                assertEquals(dimension, data.getInt(12));
            }

        }
    }
}
