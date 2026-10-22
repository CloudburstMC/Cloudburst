package org.cloudburstmc.server.level.provider.migration;

import net.daporkchop.ldbjni.LevelDB;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.level.provider.leveldb.serializer.ImportedChunkSerializer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class ImportedEntityRelocationTest {

    @Test
    void canonicalEntityNamesResolveFromTheCatalogWhileHistoricalAliasesKeepTheirMeaning() {
        CloudEntityRegistry registry = CloudEntityRegistry.get();
        Map<String, Identifier> names = Map.of(
                "Fox", Identifier.parse("minecraft:fox"),
                "GlowSquid", Identifier.parse("minecraft:glow_squid"),
                "HappyGhast", Identifier.parse("minecraft:happy_ghast"),
                "PrimedTnt", Identifier.parse("minecraft:tnt"),
                "Villager", Identifier.parse("minecraft:villager"),
                "VillagerV1", Identifier.parse("minecraft:villager_v2"));

        names.forEach((name, expected) -> {
            assertEquals(expected, registry.getIdentifier(name));
            NbtMapBuilder tag = entity("legacy name", 1, 64, 1).toBuilder();
            tag.remove("identifier");
            tag.putString("id", name);

            assertEquals(expected.toString(), ImportedEntityData.entities(List.of(tag.build()), UnaryOperator.identity())
                    .getFirst().getString("identifier"));
        });

        assertNull(registry.getIdentifier("UnregisteredTestEntity"));
        NbtMap unsupported = entity("unregistered", 1, 64, 1).toBuilder()
                .putString("identifier", "UnregisteredTestEntity").build();

        assertThrows(IllegalArgumentException.class,
                () -> ImportedEntityData.entities(List.of(unsupported), UnaryOperator.identity()));
    }

    @Test
    void movesEntitiesAcrossNegativeBoundariesAndMergesArrivalsWithoutLosingResidents(@TempDir Path directory) throws Exception {
        NbtMap firstResident = entity("first resident", 1, 64, 1);
        NbtMap firstArrival = entity("first arrival", -0.25f, 64, -0.25f);
        NbtMap secondResident = entity("second resident", -2, 64, -2);
        NbtMap returning = entity("returning", 0.25f, 64, 0.25f);
        NbtMap secondArrival = entity("second arrival", -0.5f, 64, -0.5f);

        try (DB database = open(directory)) {
            for (Vector2i chunk : List.of(Vector2i.from(0, 0), Vector2i.from(-1, -1), Vector2i.from(2, 0))) {
                database.put(LevelDBKey.VERSION.getKey(chunk.getX(), chunk.getY()), new byte[]{42});
            }

            database.put(LevelDBKey.ENTITIES.getKey(0, 0), encoded(firstResident, firstArrival));
            database.put(LevelDBKey.ENTITIES.getKey(-1, -1), encoded(secondResident, returning));
            database.put(LevelDBKey.ENTITIES.getKey(2, 0), encoded(secondArrival));

            ImportedChunkSerializer.relocateEntities(database);

            List<NbtMap> first = read(database, 0, 0);
            List<NbtMap> second = read(database, -1, -1);
            assertEquals(2, first.size());
            assertEquals(Set.of(firstResident, returning), Set.copyOf(first));
            assertEquals(3, second.size());
            assertEquals(Set.of(secondResident, firstArrival, secondArrival), Set.copyOf(second));
            assertNull(database.get(LevelDBKey.ENTITIES.getKey(2, 0)));

            ImportedChunkSerializer.relocateEntities(database);

            assertEquals(first, read(database, 0, 0));
            assertEquals(second, read(database, -1, -1));
        }
    }

    @Test
    void missingDestinationTerrainFailsBeforeChangingAnyEntityRecords(@TempDir Path directory) throws Exception {
        byte[] original = encoded(entity("moving", -0.25f, 64, -0.25f));
        try (DB database = open(directory)) {
            database.put(LevelDBKey.VERSION.getKey(0, 0), new byte[]{42});
            database.put(LevelDBKey.ENTITIES.getKey(0, 0), original);

            assertThrows(IOException.class, () -> ImportedChunkSerializer.relocateEntities(database));

            assertArrayEquals(original, database.get(LevelDBKey.ENTITIES.getKey(0, 0)));
            assertNull(database.get(LevelDBKey.ENTITIES.getKey(-1, -1)));
        }
    }

    @Test
    void entityPositionsAreNotRestrictedToTerrainHeightButMustRemainFiniteAndAddressable() {
        NbtMap entity = entity("above terrain", -0.25f, 1024, -16.25f);

        assertEquals(List.of(entity), ImportedEntityData.entities(List.of(entity), UnaryOperator.identity()));
        assertEquals(Vector2i.from(-1, -2), ImportedEntityData.chunkPosition(entity));

        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, 2147483648f}) {
            NbtMap malformed = entity("invalid", invalid, 64, 0);

            assertThrows(IllegalArgumentException.class,
                    () -> ImportedEntityData.chunkPosition(
                            ImportedEntityData.entities(List.of(malformed), UnaryOperator.identity()).getFirst()));
        }
    }

    private static DB open(Path directory) throws IOException {
        return LevelDB.PROVIDER.open(directory.resolve("db").toFile(), new Options()
                .createIfMissing(true).compressionType(CompressionType.ZLIB_RAW));
    }

    private static NbtMap entity(String label, float x, float y, float z) {
        return NbtMap.builder().putString("identifier", "minecraft:item")
                .putString("testLabel", label)
                .putList("Pos", NbtType.FLOAT, List.of(x, y, z))
                .putList("Rotation", NbtType.FLOAT, List.of(0f, 0f)).build();
    }

    private static byte[] encoded(NbtMap... records) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTOutputStream output = NbtUtils.createWriterLE(bytes)) {
            for (NbtMap record : records) {
                output.writeTag(record);
            }
        }

        return bytes.toByteArray();
    }

    private static List<NbtMap> read(DB database, int x, int z) throws IOException {
        byte[] data = database.get(LevelDBKey.ENTITIES.getKey(x, z));
        assertNotNull(data);
        List<NbtMap> records = new ArrayList<>();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             NBTInputStream input = NbtUtils.createReaderLE(bytes)) {
            while (bytes.available() > 0) {
                records.add(assertInstanceOf(NbtMap.class, input.readTag()));
            }
        }

        return records;
    }
}
