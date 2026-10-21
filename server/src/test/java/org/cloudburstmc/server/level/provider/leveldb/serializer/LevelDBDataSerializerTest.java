package org.cloudburstmc.server.level.provider.leveldb.serializer;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.CloudLevelData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private CloudLevelData data(long time) {
        CloudLevelData data = new CloudLevelData();
        data.setName("test");
        data.setSeed(1);
        data.setGenerator(Identifier.parse("cloudburst:standard"));
        data.getClock().setTotalTicks(time);
        return data;
    }
}
