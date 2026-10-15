package org.cloudburstmc.server.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapIdAllocatorTest {

    @TempDir
    private Path directory;

    @Test
    void preservesUncraftedScaleReservationsAcrossRestarts() throws IOException {
        MapIdAllocator ids = new MapIdAllocator(this.directory, 216);
        assertArrayEquals(new long[]{216, 217, 218, 219, 220}, ids.allocate(5));
        assertTrue(ids.isReserved(216));
        assertTrue(ids.isReserved(220));
        assertFalse(ids.isReserved(221));
        assertFalse(ids.isReserved(-1));

        MapIdAllocator restarted = new MapIdAllocator(this.directory, 217);
        assertArrayEquals(new long[]{221, 222, 223, 224, 225}, restarted.allocate(5));
        assertArrayEquals(new long[]{226}, restarted.allocate(1));
    }

    @Test
    void neverReusesExistingMapIdsWhenInitializingTheCounter() throws IOException {
        MapIdAllocator ids = new MapIdAllocator(this.directory, 10);
        ids.allocate(3);

        MapIdAllocator restarted = new MapIdAllocator(this.directory, 100);
        assertArrayEquals(new long[]{100, 101}, restarted.allocate(2));
        assertArrayEquals(new long[0], restarted.allocate(0));
        assertArrayEquals(new long[]{102}, restarted.allocate(1));
        assertThrows(IllegalArgumentException.class, () -> restarted.allocate(-1));
    }
}
