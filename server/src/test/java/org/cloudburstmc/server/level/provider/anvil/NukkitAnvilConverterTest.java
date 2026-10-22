package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NukkitAnvilConverterTest {

    @Test
    void extraBlockDataIsNotSilentlyDiscarded() throws IOException {
        NbtMap level = NbtMap.builder().putByteArray("ExtraData", new byte[]{0, 0, 0, 1, 0, 0}).build();
        IOException failure = reject(level);

        assertTrue(failure.getMessage().contains("ExtraData"));
    }

    private static IOException reject(NbtMap level) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTOutputStream output = NbtUtils.createWriter(bytes)) {
            output.writeTag(NbtMap.builder().putCompound("Level", level).build());
        }

        ByteBuf buffer = Unpooled.wrappedBuffer(bytes.toByteArray());
        try {
            return assertThrows(IOException.class, () -> {
                try (ImportedChunk ignored = NukkitAnvilConverter.convertToCloudburst(null, buffer, null, null, null)) {
                    fail("Unsupported Anvil records were accepted");
                }
            });
        } finally {
            buffer.release();
        }
    }
}
