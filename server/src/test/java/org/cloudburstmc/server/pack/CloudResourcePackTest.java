package org.cloudburstmc.server.pack;

import org.cloudburstmc.api.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class CloudResourcePackTest {

    @TempDir
    private Path directory;

    @Test
    void readsTheRequestedRangeFromItsByteOffset() throws Exception {
        Path source = this.archive();
        byte[] expected = Files.readAllBytes(source);

        try (CloudResourcePack pack = CloudPackLoader.load(source)) {
            assertArrayEquals(Arrays.copyOfRange(expected, 10, 20), pack.readChunk(10, 10));
        }
    }

    @Test
    void shortensTheFinalRangeWithoutPaddingIt() throws Exception {
        Path source = this.archive();
        byte[] expected = Files.readAllBytes(source);

        try (CloudResourcePack pack = CloudPackLoader.load(source)) {
            assertArrayEquals(Arrays.copyOfRange(expected, expected.length - 3, expected.length),
                    pack.readChunk(pack.getSize() - 3, Integer.MAX_VALUE));
        }
    }

    @Test
    void returnsAnEmptyRangeAtTheEndOfTheArchive() throws Exception {
        try (CloudResourcePack pack = CloudPackLoader.load(this.archive())) {
            assertArrayEquals(new byte[0], pack.readChunk(pack.getSize(), 8192));
        }
    }

    @Test
    void rejectsInvalidRangesBeforeAllocatingAChunk() throws Exception {
        try (CloudResourcePack pack = CloudPackLoader.load(this.archive())) {
            assertThrows(IllegalArgumentException.class, () -> pack.readChunk(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> pack.readChunk(0, 0));
            assertThrows(IllegalArgumentException.class, () -> pack.readChunk(0, -1));
            assertThrows(IllegalArgumentException.class, () -> pack.readChunk(Long.MAX_VALUE, 1));
        }
    }

    @Test
    void keepsTransfersAndMetadataStableWhenTheSourceChanges() throws Exception {
        Path source = this.archive();
        byte[] expected = Files.readAllBytes(source);

        try (CloudResourcePack pack = CloudPackLoader.load(source)) {
            Files.write(source, new byte[]{0});

            assertArrayEquals(expected, pack.readChunk(0, expected.length));
            assertEquals(expected.length, pack.getSize());
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(expected), pack.getHash());
        }
    }

    @Test
    void returnsAnIndependentHashArray() throws Exception {
        try (CloudResourcePack pack = CloudPackLoader.load(this.archive())) {
            byte[] expected = pack.getHash();
            byte[] modified = pack.getHash();
            Arrays.fill(modified, (byte) 0);

            assertArrayEquals(expected, pack.getHash());
        }
    }

    @Test
    void readsConcurrentRangesWithoutSharingAFilePosition() throws Exception {
        Path source = this.archive();
        byte[] expected = Files.readAllBytes(source);

        try (CloudResourcePack pack = CloudPackLoader.load(source); ExecutorService executor = Executors.newFixedThreadPool(4)) {
            Future<byte[]> first = executor.submit(() -> pack.readChunk(5, 20));
            Future<byte[]> second = executor.submit(() -> pack.readChunk(30, 40));

            assertArrayEquals(Arrays.copyOfRange(expected, 5, 25), first.get());
            assertArrayEquals(Arrays.copyOfRange(expected, 30, 70), second.get());
        }
    }

    @Test
    void closesTransferAccessWithoutDeletingTheSource() throws Exception {
        Path source = this.archive();
        CloudResourcePack pack = CloudPackLoader.load(source);
        pack.close();
        pack.close();

        assertThrows(IOException.class, () -> pack.readChunk(0, 1));
        assertTrue(Files.exists(source));
    }

    @Test
    void archivesNestedDirectoryAssetsWithPortableNamesAndTheirContents() throws Exception {
        Path source = CloudPackTestFiles.directory(this.directory.resolve("source"), UUID.randomUUID(), "");

        try (CloudResourcePack pack = CloudPackLoader.load(source);
             ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pack.readChunk(0, (int) pack.getSize())))) {
            boolean found = false;
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().equals("textures/blocks/example.bin")) {
                    assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, zip.readAllBytes());
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    void rejectsMissingManifests() throws Exception {
        Path source = Files.createDirectory(this.directory.resolve("empty"));

        assertThrows(IOException.class, () -> {
            try (CloudResourcePack pack = CloudPackLoader.load(source)) {
                fail("Loaded a pack without a manifest: " + pack.getName());
            }
        });
    }

    @Test
    void rejectsUnsupportedModuleTypes() throws Exception {
        Path source = CloudPackTestFiles.directory(this.directory.resolve("source"), UUID.randomUUID(), "");
        Path manifest = source.resolve("manifest.json");
        ObjectNode json = (ObjectNode) Server.JSON_MAPPER.readTree(Files.readString(manifest));
        ((ObjectNode) json.get("modules").get(0)).put("type", "data");
        Files.writeString(manifest, Server.JSON_MAPPER.writeValueAsString(json));

        assertThrows(IOException.class, () -> {
            try (CloudResourcePack pack = CloudPackLoader.load(source)) {
                fail("Loaded a pack with an unsupported module type: " + pack.getName());
            }
        });
    }

    @Test
    void rejectsAnEmptyModuleList() throws Exception {
        Path source = CloudPackTestFiles.directory(this.directory.resolve("source"), UUID.randomUUID(), "");
        Path manifest = source.resolve("manifest.json");
        ObjectNode json = (ObjectNode) Server.JSON_MAPPER.readTree(Files.readString(manifest));
        json.putArray("modules");
        Files.writeString(manifest, Server.JSON_MAPPER.writeValueAsString(json));

        assertThrows(IOException.class, () -> {
            try (CloudResourcePack pack = CloudPackLoader.load(source)) {
                fail("Loaded a pack with no modules: " + pack.getName());
            }
        });
    }

    @Test
    void ignoresUnneededManifestMetadata() throws Exception {
        Path source = CloudPackTestFiles.directory(this.directory.resolve("source"), UUID.randomUUID(), "");
        Path manifest = source.resolve("manifest.json");
        ObjectNode json = (ObjectNode) Server.JSON_MAPPER.readTree(Files.readString(manifest));
        json.putObject("metadata").putArray("authors").add("Example");
        Files.writeString(manifest, Server.JSON_MAPPER.writeValueAsString(json));

        try (CloudResourcePack pack = CloudPackLoader.load(source)) {
            assertEquals("Example", pack.getName());
        }
    }

    private Path archive() throws IOException {
        Path source = CloudPackTestFiles.directory(this.directory.resolve("source"), UUID.randomUUID(), "");
        return CloudPackTestFiles.archive(source, this.directory.resolve("source.mcpack"));
    }
}
