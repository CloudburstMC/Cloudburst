package org.cloudburstmc.server.level.provider.leveldb.serializer;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.CloudLevelData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBDataSerializerTest {

    @TempDir
    Path directory;

    @Test
    void vanillaFlatMetadataDoesNotReplaceConfiguredGeneratorOptions() throws Exception {
        String flatPreset = "{\"biome_id\":1,\"encoding_version\":6,\"block_layers\":[]}";
        writeMetadata(NbtMap.builder().putInt("Generator", 1)
                .putString("FlatWorldLayers", flatPreset).putLong("RandomSeed", 42).build());
        CloudLevelData initial = data(0);
        initial.setGeneratorOptions("overworld");

        CloudLevelData loaded = LevelDBDataSerializer.INSTANCE.load(initial, directory).orElseThrow();

        assertEquals(initial.getGenerator(), loaded.getGenerator());
        assertEquals("overworld", loaded.getGeneratorOptions());
        assertEquals(42, loaded.getSeed());
        assertEquals(flatPreset, loaded.getAdditionalData().get("FlatWorldLayers"));

        LevelDBDataSerializer.INSTANCE.save(loaded, directory);
        NbtMap saved = readMetadata();
        assertEquals("overworld", saved.getString("generatorOptions"));
        assertEquals(flatPreset, saved.getString("FlatWorldLayers"));
        assertEquals("overworld", LevelDBDataSerializer.INSTANCE.load(data(0), directory).orElseThrow().getGeneratorOptions());
    }

    @Test
    void nativeGeneratorOptionsAreSavedSeparatelyFromBedrockFlatPresets() throws Exception {
        CloudLevelData saved = data(0);
        saved.setGeneratorOptions("overworld");

        LevelDBDataSerializer.INSTANCE.save(saved, directory);

        NbtMap tag = readMetadata();
        assertEquals("cloudburst:standard", tag.getString("generatorName"));
        assertEquals("overworld", tag.getString("generatorOptions"));
        assertFalse(tag.containsKey("FlatWorldLayers"));
    }

    @Test
    void olderNativeOptionsAreMigratedAtThePersistenceBoundary() throws Exception {
        writeMetadata(NbtMap.builder().putString("generatorName", "cloudburst:standard")
                .putString("FlatWorldLayers", "nether").build());

        CloudLevelData loaded = LevelDBDataSerializer.INSTANCE.load(data(0), directory).orElseThrow();
        assertEquals("nether", loaded.getGeneratorOptions());

        LevelDBDataSerializer.INSTANCE.save(loaded, directory);
        assertEquals("nether", readMetadata().getString("generatorOptions"));
    }

    @Test
    void explicitNativeOptionsTakePrecedenceOverTheOldField() throws Exception {
        writeMetadata(NbtMap.builder().putString("generatorName", "cloudburst:standard")
                .putString("generatorOptions", "overworld").putString("FlatWorldLayers", "{\"encoding_version\":6}").build());

        assertEquals("overworld", LevelDBDataSerializer.INSTANCE.load(data(0), directory).orElseThrow().getGeneratorOptions());
    }

    @Test
    void malformedNativeOptionsAreNotSilentlyIgnored() throws Exception {
        writeMetadata(NbtMap.builder().putString("generatorName", "cloudburst:standard")
                .putInt("generatorOptions", 1).build());

        assertThrows(IOException.class, () -> LevelDBDataSerializer.INSTANCE.load(data(0), directory));
    }

    @Test
    void savedMetadataHasAnExactLittleEndianPayloadLength() throws Exception {
        LevelDBDataSerializer.INSTANCE.save(data(10), directory);
        byte[] bytes = Files.readAllBytes(directory.resolve("level.dat"));

        assertEquals(bytes.length - 8, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(4));
        assertEquals(10, LevelDBDataSerializer.INSTANCE.load(data(0), directory).orElseThrow().getClock().getTotalTicks());
        assertFalse(Files.exists(directory.resolve("level.dat_old")));
    }

    @Test
    void invalidPayloadLengthRecoversThePreviousValidatedSave() throws Exception {
        LevelDBDataSerializer.INSTANCE.save(data(10), directory);
        LevelDBDataSerializer.INSTANCE.save(data(20), directory);
        Path file = directory.resolve("level.dat");
        byte[] damaged = Files.readAllBytes(file);
        ByteBuffer.wrap(damaged).order(ByteOrder.LITTLE_ENDIAN).putInt(4, damaged.length);
        Files.write(file, damaged);

        assertEquals(10, LevelDBDataSerializer.INSTANCE.load(data(0), directory).orElseThrow().getClock().getTotalTicks());
        assertArrayEquals(Files.readAllBytes(directory.resolve("level.dat_old")), Files.readAllBytes(file));
    }

    @Test
    void clockStateRoundTripsIndependentlyOfGameTime() throws Exception {
        CloudLevelData saved = data(49000);
        saved.setGameTime(1234);
        saved.getClock().setRate(0.5f);
        saved.getClock().setPartialTick(0.75f);
        saved.getClock().setPaused(true);

        LevelDBDataSerializer.INSTANCE.save(saved, directory);
        CloudLevelData loaded = LevelDBDataSerializer.INSTANCE.load(data(0), directory).orElseThrow();

        assertEquals(1234, loaded.getGameTime());
        assertEquals(49000, loaded.getClock().getTotalTicks());
        assertEquals(0.5f, loaded.getClock().getRate());
        assertEquals(0.75f, loaded.getClock().getPartialTick());
        assertTrue(loaded.getClock().isPaused());
    }

    @Test
    void existingCorruptMetadataNeverBecomesAnAbsentLevel() throws Exception {
        Path file = directory.resolve("level.dat");
        Files.writeString(file, "corrupt");

        assertThrows(IOException.class, () -> LevelDBDataSerializer.INSTANCE.load(data(0), directory));
        assertEquals("corrupt", Files.readString(file));
    }

    private void writeMetadata(NbtMap tag) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTOutputStream output = NbtUtils.createWriterLE(bytes)) {
            output.writeTag(tag);
        }

        byte[] payload = bytes.toByteArray();
        Files.write(directory.resolve("level.dat"), ByteBuffer.allocate(8 + payload.length)
                .order(ByteOrder.LITTLE_ENDIAN).putInt(10).putInt(payload.length).put(payload).array());
    }

    private NbtMap readMetadata() throws IOException {
        byte[] bytes = Files.readAllBytes(directory.resolve("level.dat"));
        try (NBTInputStream input = NbtUtils.createReaderLE(new ByteArrayInputStream(bytes, 8, bytes.length - 8))) {
            return assertInstanceOf(NbtMap.class, input.readTag());
        }
    }

    private CloudLevelData data(long time) {
        CloudLevelData data = new CloudLevelData();
        data.setName("test");
        data.setSeed(1);
        data.setGenerator(Identifier.parse("cloudburst:standard"));
        data.getClock().setTotalTicks(time);
        return data;
    }
}
