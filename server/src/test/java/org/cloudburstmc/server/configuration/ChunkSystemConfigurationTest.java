package org.cloudburstmc.server.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkSystemConfigurationTest {

    @Test
    void automaticThreadCountsReserveCapacityForTheServerThread() {
        GlobalConfiguration.ChunkSystem settings = new GlobalConfiguration.ChunkSystem();

        assertEquals(1, settings.resolveIoThreads());
        assertEquals(1, settings.resolveWorkerThreads(1));
        assertEquals(1, settings.resolveWorkerThreads(6));
        assertEquals(1, settings.resolveWorkerThreads(7));
        assertEquals(2, settings.resolveWorkerThreads(8));
        assertEquals(2, settings.resolveWorkerThreads(9));
        assertEquals(2, settings.resolveWorkerThreads(10));
        assertEquals(3, settings.resolveWorkerThreads(12));
        assertEquals(4, settings.resolveWorkerThreads(16));
        assertEquals(8, settings.resolveWorkerThreads(32));
    }

    @Test
    void positiveCountsOverrideAutomaticSizing() {
        GlobalConfiguration.ChunkSystem settings = GlobalConfiguration.ChunkSystem.builder()
                .ioThreads(3)
                .workerThreads(5)
                .build();

        assertEquals(3, settings.resolveIoThreads());
        assertEquals(5, settings.resolveWorkerThreads(16));
    }

    @Test
    void zeroCountsAlsoSelectAutomaticSizing() {
        GlobalConfiguration.ChunkSystem settings = GlobalConfiguration.ChunkSystem.builder()
                .ioThreads(0)
                .workerThreads(0)
                .build();

        assertEquals(1, settings.resolveIoThreads());
        assertEquals(4, settings.resolveWorkerThreads(16));
    }
}
