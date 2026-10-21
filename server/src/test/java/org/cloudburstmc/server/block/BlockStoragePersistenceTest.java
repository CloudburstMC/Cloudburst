package org.cloudburstmc.server.block;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockStoragePersistenceTest {

    @BeforeAll
    static void initializeRegistry() {
        assertNotNull(BlockTestRegistry.get());
    }

    @Test
    void numericMetadataCannotBeSwallowedOrSubstitutedWithAir() throws IOException {
        NbtMap legacy = NbtMap.builder()
                .putString("name", "minecraft:stone")
                .putShort("val", (short) 1)
                .build();

        for (boolean singleton : new boolean[]{true, false}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(singleton ? 0 : 2);
                if (!singleton) {
                    buffer.writeZero(4096 / 32 * Integer.BYTES);
                    buffer.writeIntLE(1);
                }

                try (NBTOutputStream output = NbtUtils.createWriterLE(new ByteBufOutputStream(buffer))) {
                    output.writeTag(legacy);
                }

                assertThrows(UnsupportedOperationException.class, () -> BlockStorage.readFromStorage(buffer));
            } finally {
                buffer.release();
            }
        }
    }
}
