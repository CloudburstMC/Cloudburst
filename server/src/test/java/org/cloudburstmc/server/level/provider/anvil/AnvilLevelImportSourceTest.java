package org.cloudburstmc.server.level.provider.anvil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class AnvilLevelImportSourceTest {

    @Test
    void emptyRegionsCompleteWithoutCreatingChunks(@TempDir Path directory) throws Exception {
        Path regions = Files.createDirectory(directory.resolve("region"));
        Files.write(regions.resolve("r.0.0.mca"), new byte[8192]);

        try (AnvilLevelImportSource source = new AnvilLevelImportSource(directory, Runnable::run)) {
            source.visitChunks((x, z) -> {
                throw new AssertionError("Empty regions must not create chunk builders");
            }, chunk -> {
                throw new AssertionError("Empty regions must not emit chunks");
            }).join();
        }
    }

    @Test
    void openingMissingSourceDoesNotCreateDirectories(@TempDir Path directory) {
        Path source = directory.resolve("missing");

        assertThrows(IOException.class, () -> {
            try (AnvilLevelImportSource ignored = new AnvilLevelImportSource(source, Runnable::run)) {
                fail("A missing import source must not open successfully");
            }
        });
        assertFalse(Files.exists(source));
    }

    @Test
    void overflowingRegionCoordinatesFailBeforeVisitingChunks(@TempDir Path directory) throws Exception {
        for (String name : new String[]{"r.67108864.0.mca", "r.0.-67108865.mca"}) {
            Path level = Files.createDirectory(directory.resolve(name));
            Path regions = Files.createDirectory(level.resolve("region"));
            Files.write(regions.resolve(name), new byte[8192]);

            try (AnvilLevelImportSource source = new AnvilLevelImportSource(level, Runnable::run)) {
                CompletionException failure = assertThrows(CompletionException.class, () -> source.visitChunks(
                        (x, z) -> {
                            throw new AssertionError("Overflowing coordinates must not reach the chunk factory");
                        }, chunk -> {
                            throw new AssertionError("No chunk should be consumed");
                        }).join());

                IOException cause = assertInstanceOf(IOException.class, failure.getCause());
                assertTrue(cause.getMessage().contains("Region coordinates are out of range"));
                assertInstanceOf(ArithmeticException.class, cause.getCause());
            }
        }
    }

    @Test
    void invalidRegionNameFailsInsteadOfSkippingData(@TempDir Path directory) throws Exception {
        Path regions = Files.createDirectory(directory.resolve("region"));
        Path region = regions.resolve("invalid.mca");
        Files.write(region, new byte[8192]);

        try (AnvilLevelImportSource source = new AnvilLevelImportSource(directory, Runnable::run)) {
            CompletionException failure = assertThrows(CompletionException.class, () -> source.visitChunks(
                    (x, z) -> {
                        throw new AssertionError("No chunk should be decoded from an invalid region name");
                    }, chunk -> {
                        throw new AssertionError("No chunk should be consumed");
                    }).join());

            assertInstanceOf(IOException.class, failure.getCause());
        }

        assertEquals(8192, Files.size(region));
        Files.delete(region);
    }
}
