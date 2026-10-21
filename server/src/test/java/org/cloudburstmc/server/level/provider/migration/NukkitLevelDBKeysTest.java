package org.cloudburstmc.server.level.provider.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukkitLevelDBKeysTest {

    @Test
    void preservesCoordinatesDimensionsAndSignedSectionY() {
        for (int dimension : new int[]{0, 1, 2}) {
            byte[] key = NukkitLevelDBKeys.key(-123456, 654321, dimension, ',');
            byte[] section = NukkitLevelDBKeys.section(-123456, 654321, dimension, -4);
            NukkitLevelDBKeys.ChunkAddress expected = new NukkitLevelDBKeys.ChunkAddress(-123456, 654321, dimension);

            assertEquals(dimension == 0 ? 9 : 13, key.length);
            assertEquals(expected, NukkitLevelDBKeys.chunk(key).orElseThrow());
            assertEquals(expected, NukkitLevelDBKeys.chunk(section).orElseThrow());
            assertEquals(-4, section[section.length - 1]);
        }
    }

    @Test
    void ignoresNonChunkKeysAndMalformedSuffixes() {
        assertTrue(NukkitLevelDBKeys.chunk(new byte[0]).isEmpty());
        assertTrue(NukkitLevelDBKeys.chunk(NukkitLevelDBKeys.key(1, 2, 0, '/')).isEmpty());
        assertTrue(NukkitLevelDBKeys.chunk(NukkitLevelDBKeys.key(1, 2, 0, '?')).isEmpty());
    }
}
