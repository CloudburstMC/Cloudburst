package org.cloudburstmc.server.level.provider.migration;

import net.daporkchop.ldbjni.LevelDB;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.CloudLevelData;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBImportSourceTest {

    @Test
    void acceptsAnEmptyDimensionAndPreservesItsMetadataWithoutChangingTheSource(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of());
        Map<String, byte[]> before = files(directory);
        CloudLevelData initial = new CloudLevelData();
        initial.setName("nether");
        initial.setSeed(27);
        initial.setGenerator(Identifier.parse("cloudburst:standard"));
        initial.setGeneratorOptions("nether");

        try (LevelDBImportSource source = new LevelDBImportSource(directory, 1, null, Runnable::run)) {
            CloudLevelData loaded = source.loadLevelData(initial).join();
            assertTrue(source.storedDimensions().isEmpty());
            assertEquals(901, loaded.getSeed());
            assertEquals(1, loaded.getDimension());

            source.visitChunks((x, z) -> {
                throw new AssertionError("An empty database must not create chunk builders");
            }, chunk -> {
                throw new AssertionError("An empty database must not emit chunks");
            }).join();
        }

        Map<String, byte[]> after = files(directory);
        assertEquals(before.keySet(), after.keySet());
        before.forEach((name, content) -> assertArrayEquals(content, after.get(name), name));
    }

    @Test
    void auxiliaryRecordsWithoutVersionsDoNotDiscoverChunks(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of(
                LevelDBImportKeys.key(2, -6, 0, '1'), new byte[]{1},
                LevelDBImportKeys.key(3, -5, 0, '1'), new byte[]{2},
                LevelDBImportKeys.actorIndex(-3, 1, 0), new byte[0],
                LevelDBImportKeys.key(10, 0, 0, ','), new byte[]{42}));
        IllegalStateException reachedStoredChunk = new IllegalStateException("Reached stored chunk");

        try (LevelDBImportSource source = new LevelDBImportSource(directory, 0, null, Runnable::run)) {
            CompletionException failure = assertThrows(CompletionException.class, () -> source.visitChunks((x, z) -> {
                assertEquals(10, x);
                assertEquals(0, z);
                throw reachedStoredChunk;
            }, chunk -> {
                throw new AssertionError("The chunk builder must stop before decoding");
            }).join());

            assertSame(reachedStoredChunk, failure.getCause());
        }
    }

    @Test
    void aDifferentStoredDimensionCannotBecomeAnEmptySuccessfulImport(@TempDir Path directory) throws Exception {
        for (int dimension : new int[]{0, 2}) {
            Path world = directory.resolve("dimension-" + dimension);
            writeWorld(world, Map.of(LevelDBImportKeys.key(0, 0, dimension, ','), new byte[]{8}));

            try (LevelDBImportSource source = new LevelDBImportSource(world, 1, null, Runnable::run)) {
                IOException failure = assertInstanceOf(IOException.class, failedVisit(source).getCause());

                assertTrue(failure.getMessage().contains("configured dimension 1"));
                assertTrue(failure.getMessage().contains("Stored dimensions: [" + dimension + "]"));
            }
        }
    }

    @Test
    void discoversAllStoredDimensionsWithoutTreatingAuxiliaryRecordsAsTerrain(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of(
                LevelDBImportKeys.key(0, 0, 0, ','), new byte[]{42},
                LevelDBImportKeys.key(1, 2, 1, 'v'), new byte[]{42},
                LevelDBImportKeys.key(2, 3, 2, ','), new byte[]{42},
                LevelDBImportKeys.actorIndex(4, 5, 7), new byte[0]));

        try (LevelDBImportSource source = new LevelDBImportSource(directory, 0, null, Runnable::run)) {
            assertEquals(Set.of(0, 1, 2), source.storedDimensions());
        }
    }

    @Test
    void readsOnlyTheSelectedDimensionAndLoadsTheSharedSeed(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of(
                LevelDBImportKeys.key(0, 0, 0, ','), new byte[]{42},
                LevelDBImportKeys.key(1, 2, 1, ','), new byte[]{42},
                LevelDBImportKeys.key(2, 3, 2, ','), new byte[]{42}));

        for (int dimension : new int[]{0, 1, 2}) {
            IllegalStateException reachedStoredChunk = new IllegalStateException("Reached selected dimension");
            CloudLevelData initial = new CloudLevelData();
            initial.setName("dimension-" + dimension);
            initial.setSeed(27);
            initial.setGenerator(Identifier.parse("cloudburst:standard"));

            try (LevelDBImportSource source = new LevelDBImportSource(directory, dimension, null, Runnable::run)) {
                CloudLevelData loaded = source.loadLevelData(initial).join();
                assertEquals(901, loaded.getSeed());
                assertEquals(dimension, loaded.getDimension());

                CompletionException failure = assertThrows(CompletionException.class, () -> source.visitChunks((x, z) -> {
                    assertEquals(dimension, x);
                    assertEquals(dimension == 0 ? 0 : dimension + 1, z);
                    throw reachedStoredChunk;
                }, chunk -> {
                    throw new AssertionError("The chunk builder must stop before decoding");
                }).join());

                assertSame(reachedStoredChunk, failure.getCause());
            }
        }
    }

    @Test
    void dimensionViewsShareOneOwnerAndClosingAViewKeepsOtherDimensionsAvailable(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of(
                LevelDBImportKeys.key(0, 0, 0, ','), new byte[]{42},
                LevelDBImportKeys.key(1, 2, 1, ','), new byte[]{42}));

        try (LevelDBImportSource source = new LevelDBImportSource(directory, 0, null, Runnable::run)) {
            try (LevelDBImportSource nether = source.forDimension(1)) {
                CloudLevelData initial = new CloudLevelData();
                initial.setName("nether");
                initial.setSeed(27);
                initial.setGenerator(Identifier.parse("cloudburst:standard"));
                CloudLevelData loaded = nether.loadLevelData(initial).join();

                assertEquals(1, loaded.getDimension());
                assertEquals(901, loaded.getSeed());
            }

            assertEquals(Set.of(0, 1), source.storedDimensions());
            assertThrows(IllegalArgumentException.class, () -> source.forDimension(3));
        }
    }

    @Test
    void ownerClosureInvalidatesAllDimensionViews(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of());

        try (LevelDBImportSource source = new LevelDBImportSource(directory, 0, null, Runnable::run);
             LevelDBImportSource view = source.forDimension(1)) {
            source.close();

            assertThrows(IllegalStateException.class, view::storedDimensions);
            assertThrows(IllegalStateException.class, () -> view.forDimension(2));
        }
    }

    @Test
    void unsupportedRecordsCannotBecomeAnEmptySuccessfulImport(@TempDir Path directory) throws Exception {
        writeWorld(directory, Map.of(new byte[]{1}, new byte[]{2}));

        try (LevelDBImportSource source = new LevelDBImportSource(directory, 1, null, Runnable::run)) {
            assertThrows(IOException.class, source::storedDimensions);
            IOException failure = assertInstanceOf(IOException.class, failedVisit(source).getCause());

            assertTrue(failure.getMessage().contains("no supported chunk keys"));
        }
    }

    private static CompletionException failedVisit(LevelDBImportSource source) {
        return assertThrows(CompletionException.class, () -> source.visitChunks((x, z) -> {
            throw new AssertionError("No selected chunk should be decoded");
        }, chunk -> {
            throw new AssertionError("No selected chunk should be consumed");
        }).join());
    }

    private static void writeWorld(Path world, Map<byte[], byte[]> records) throws IOException {
        Files.createDirectories(world);
        try (DB database = LevelDB.PROVIDER.open(world.resolve("db").toFile(), new Options()
                .createIfMissing(true).compressionType(CompressionType.ZLIB_RAW))) {
            records.forEach(database::put);
        }

        NbtMap metadata = NbtMap.builder().putString("generatorName", "normal")
                .putString("generatorOptions", "").putInt("PM1EGen", 2).putLong("RandomSeed", 901).build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTOutputStream output = NbtUtils.createWriterLE(bytes)) {
            output.writeTag(metadata);
        }

        byte[] payload = bytes.toByteArray();
        Files.write(world.resolve("level.dat"), ByteBuffer.allocate(8 + payload.length)
                .order(ByteOrder.LITTLE_ENDIAN).putInt(10).putInt(payload.length).put(payload).array());
    }

    private static Map<String, byte[]> files(Path world) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (Stream<Path> paths = Files.walk(world)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                result.put(world.relativize(path).toString(), Files.readAllBytes(path));
            }
        }

        return result;
    }
}
