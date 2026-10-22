package org.cloudburstmc.server.level.provider.leveldb;

import net.daporkchop.ldbjni.LevelDB;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.provider.LevelImportFormat;
import org.cloudburstmc.server.level.provider.leveldb.serializer.LevelDBDataSerializer;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBInitializationTest {

    @Test
    void persistsNewLevelIdentityWithoutWaitingForAutosave(@TempDir Path directory) throws Exception {
        Path world = directory.resolve("world");
        try (LevelDBProvider provider = new LevelDBProvider("world", directory, initialData(), Runnable::run)) {
            NbtMap metadata = LevelDBDataSerializer.readMetadata(world.resolve("level.dat"));

            assertEquals(LevelDBDataSerializer.CLOUDBURST_STORAGE_VERSION, metadata.getInt("CloudburstStorageVersion"));
            assertEquals(42, metadata.getLong("RandomSeed"));
            assertNull(LevelImportFormat.detect(world, null));
            assertTrue(LevelDBProviderFactory.INSTANCE.isCompatible("world", directory));
            assertEquals(42, provider.loadLevelData(initialData()).join().orElseThrow().getSeed());
        }

        assertTrue(Files.isRegularFile(world.resolve("level.dat")));
        assertNull(LevelImportFormat.detect(world, null));
    }

    @Test
    void missingMetadataOnlyAllowsAnEmptyDatabaseAndInspectionDoesNotChangeTheSource(@TempDir Path directory) throws Exception {
        for (boolean populated : new boolean[]{false, true}) {
            String name = populated ? "populated" : "empty";
            Path world = Files.createDirectory(directory.resolve(name));
            try (DB database = LevelDB.PROVIDER.open(world.resolve("db").toFile(), new Options()
                    .createIfMissing(true).compressionType(CompressionType.ZLIB_RAW))) {
                if (populated) {
                    database.put(new byte[]{1}, new byte[]{2});
                }
            }

            Map<String, byte[]> before = databaseFiles(world);
            if (populated) {
                assertThrows(IOException.class, () -> LevelImportFormat.detect(world, null));
                assertThrows(IOException.class, () -> {
                    try (LevelDBProvider ignored = new LevelDBProvider(name, directory, initialData(), Runnable::run)) {
                        fail("A populated database without metadata must not be initialized");
                    }
                });
            } else {
                assertNull(LevelImportFormat.detect(world, null));
            }

            Map<String, byte[]> after = databaseFiles(world);
            assertEquals(before.keySet(), after.keySet());
            before.forEach((file, content) -> assertArrayEquals(content, after.get(file), file));
            assertFalse(Files.exists(world.resolve("level.dat")));
            assertFalse(Files.exists(world.resolve("level.dat_old")));

            if (!populated) {
                try (LevelDBProvider provider = new LevelDBProvider(name, directory, initialData(), Runnable::run)) {
                    assertEquals(42, provider.loadLevelData(initialData()).join().orElseThrow().getSeed());
                }

                assertNull(LevelImportFormat.detect(world, null));
            }
        }
    }

    @Test
    void corruptDatabaseWithoutMetadataCannotBeTreatedAsANewLevel(@TempDir Path directory) throws Exception {
        Path database = Files.createDirectory(directory.resolve("db"));
        Files.writeString(database.resolve("CURRENT"), "missing manifest\n");

        assertThrows(IOException.class, () -> LevelImportFormat.detect(directory, null));
        assertFalse(Files.exists(directory.resolve("level.dat")));
    }

    @Test
    void recognizesMetadataWrittenBeforeDatabaseCreation(@TempDir Path directory) throws IOException {
        Path world = directory.resolve("world");
        LevelDBDataSerializer.INSTANCE.save(initialData(), world);

        assertTrue(LevelDBProviderFactory.INSTANCE.isCompatible("world", directory));
        assertFalse(Files.exists(world.resolve("db")));
    }

    private static CloudLevelData initialData() {
        CloudLevelData data = new CloudLevelData();
        data.setName("world");
        data.setSeed(42);
        data.setGenerator(Identifier.parse("cloudburst:standard"));
        data.setGeneratorOptions("overworld");
        return data;
    }

    private static Map<String, byte[]> databaseFiles(Path world) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (Stream<Path> files = Files.list(world.resolve("db"))) {
            for (Path file : files.toList()) {
                result.put(file.getFileName().toString(), Files.readAllBytes(file));
            }
        }

        return result;
    }
}
