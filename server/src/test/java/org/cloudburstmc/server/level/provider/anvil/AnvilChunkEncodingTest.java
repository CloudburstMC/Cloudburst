package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AnvilChunkEncodingTest {

    @Test
    void acceptsSupportedNumericBedrockChunkHeaders() throws IOException {
        for (int version : new int[]{1, 4}) {
            NbtMap chunk = NbtMap.builder().putByte("V", (byte) version).build();

            assertEquals(chunk, decode(NbtMap.builder().putCompound("Level", chunk).build()));
        }
    }

    @Test
    void javaChunksCannotAccidentallyUseNukkitNumericMappings() {
        assertThrows(IOException.class, () -> decode(NbtMap.builder().putCompound("Level", NbtMap.EMPTY).build()));
        assertThrows(IOException.class, () -> decode(NbtMap.builder().putInt("DataVersion", 4189)
                .putCompound("Level", NbtMap.builder().putByte("V", (byte) 1).build()).build()));
    }

    private static NbtMap decode(NbtMap root) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        try {
            try (NBTOutputStream output = NbtUtils.createWriter(new ByteBufOutputStream(buffer))) {
                output.writeTag(root);
            }

            return NukkitAnvilConverter.readChunk(buffer);
        } finally {
            buffer.release();
        }
    }
}
