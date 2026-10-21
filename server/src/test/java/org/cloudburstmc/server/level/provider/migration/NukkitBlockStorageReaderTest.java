package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NukkitBlockStorageReaderTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void convertsUnsignedNumericIdsAndBothMetadataNibbles() throws IOException {
        byte[] data = new byte[1 + 4096 + 2048];
        data[0] = 0;
        data[1] = 35;
        data[2] = 35;
        data[3] = (byte) 159;
        data[4097] = (byte) 0x21;
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());

        BlockStorage[] layers = reader.readSection(data, 0);

        assertSame(BlockTypes.ORANGE_WOOL, layers[0].getBlock(0).getType());
        assertSame(BlockTypes.MAGENTA_WOOL, layers[0].getBlock(1).getType());
        assertSame(BlockTypes.WHITE_TERRACOTTA, layers[0].getBlock(2).getType());
        assertTrue(layers[1].isEmpty());
        assertThrows(IOException.class, () -> reader.readSection(new byte[]{0, 1}, 0));
    }

    @Test
    void readsBothNamedLayersAndValidatesVersionNineSectionY() throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        byte[] data;
        try {
            buffer.writeByte(9).writeByte(2).writeByte(-4);
            writeSingleton(buffer, BlockStateCodec.write(REGISTRY.getBlock(BlockTypes.STONE)));
            writeSingleton(buffer, BlockStateCodec.write(REGISTRY.getBlock(BlockTypes.WATER)));
            data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
        } finally {
            buffer.release();
        }

        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());
        BlockStorage[] layers = reader.readSection(data, -4);

        assertSame(BlockTypes.STONE, layers[0].getBlock(4095).getType());
        assertSame(BlockTypes.WATER, layers[1].getBlock(4095).getType());
        assertThrows(IOException.class, () -> reader.readSection(data, 0));
        assertThrows(IOException.class, () -> reader.readSection(new byte[]{8, 3}, 0));
    }

    private static void writeSingleton(ByteBuf buffer, NbtMap state) throws IOException {
        buffer.writeByte(0);
        try (NBTOutputStream output = NbtUtils.createWriterLE(new ByteBufOutputStream(buffer))) {
            output.writeTag(state);
        }
    }
}
