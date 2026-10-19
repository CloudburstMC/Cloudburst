package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RegionFileTest {

    @Test
    void readsCompressedChunksWithoutChangingTheSource(@TempDir Path directory) throws Exception {
        byte[] payload = {1, 2, 3, 4};
        byte[] source = region(payload);
        Path path = directory.resolve("r.0.0.mca");
        Files.write(path, source);

        try (RegionFile region = new RegionFile(path)) {
            assertTrue(region.hasChunk(0, 0));
            assertFalse(region.hasChunk(1, 0));

            ByteBuf chunk = region.readChunk(0, 0);
            try {
                byte[] decoded = new byte[chunk.readableBytes()];
                chunk.readBytes(decoded);
                assertArrayEquals(payload, decoded);
                assertTrue(chunk.isReadOnly());
            } finally {
                chunk.release();
            }
        }

        assertArrayEquals(source, Files.readAllBytes(path));
    }

    @Test
    void rejectsTruncatedFilesWithoutPaddingThem(@TempDir Path directory) throws Exception {
        Path path = directory.resolve("r.0.0.mca");
        byte[] source = new byte[4096];
        Files.write(path, source);

        assertThrows(IOException.class, () -> {
            try (RegionFile ignored = new RegionFile(path)) {
                fail("A truncated region must not open successfully");
            }
        });

        assertArrayEquals(source, Files.readAllBytes(path));
        Files.delete(path);
    }

    @Test
    void rejectsOverlappingChunkAllocations(@TempDir Path directory) throws Exception {
        byte[] source = region(new byte[]{1});
        ByteBuffer.wrap(source).putInt(4, (2 << 8) | 1);
        Path path = directory.resolve("r.0.0.mca");
        Files.write(path, source);

        assertThrows(IOException.class, () -> {
            try (RegionFile ignored = new RegionFile(path)) {
                fail("Overlapping chunk allocations must be rejected");
            }
        });

        assertArrayEquals(source, Files.readAllBytes(path));
        Files.delete(path);
    }

    @Test
    void rejectsChunkLengthsOutsideTheirAllocation(@TempDir Path directory) throws Exception {
        byte[] source = region(new byte[]{1});
        ByteBuffer.wrap(source).putInt(8192, 4096);
        Path path = directory.resolve("r.0.0.mca");
        Files.write(path, source);

        try (RegionFile region = new RegionFile(path)) {
            assertThrows(IOException.class, () -> region.readChunk(0, 0));
        }

        assertArrayEquals(source, Files.readAllBytes(path));
    }

    @Test
    void missingRegionIsNotCreated(@TempDir Path directory) {
        Path path = directory.resolve("r.0.0.mca");

        assertThrows(IOException.class, () -> {
            try (RegionFile ignored = new RegionFile(path)) {
                fail("A missing region must not open successfully");
            }
        });
        assertFalse(Files.exists(path));
    }

    private static byte[] region(byte[] payload) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream stream = new DeflaterOutputStream(compressed)) {
            stream.write(payload);
        }

        byte[] bytes = compressed.toByteArray();
        assertTrue(bytes.length + 5 <= 4096);

        ByteBuffer region = ByteBuffer.allocate(4096 * 3);
        region.putInt((2 << 8) | 1);
        region.position(8192);
        region.putInt(bytes.length + 1);
        region.put((byte) 2);
        region.put(bytes);
        return region.array();
    }
}
