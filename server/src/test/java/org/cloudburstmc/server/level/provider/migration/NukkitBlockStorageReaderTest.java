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
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NukkitBlockStorageReaderTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void readsPackedAnvilLayersWithoutChangingAxesOrMetadata() throws IOException {
        int index = CloudChunkSection.blockIndex(3, 5, 7);
        int[] primary = new int[4096];
        int[] secondary = new int[4096];
        primary[index] = 35 << 6 | 1;
        secondary[index] = 9 << 6;
        NbtMap section = NbtMap.builder().putByte("Y", (byte) 0)
                .putIntArray("layer1", primary).putIntArray("layer2", secondary).build();
        NukkitBlockStateConverter blocks = new NukkitBlockStateConverter();
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(blocks);

        for (int version = 2; version <= 4; version++) {
            BlockStorage[] layers = reader.readAnvilSection(section, version);
            assertSame(blocks.read(35, 1), layers[0].getBlock(index));
            assertSame(blocks.read(9, 0), layers[1].getBlock(index));
            assertSame(BlockTypes.AIR, layers[0].getBlock(CloudChunkSection.blockIndex(5, 3, 7)).getType());
        }
    }

    @Test
    void convertsByteArrayAnvilCoordinatesToXzy() throws IOException {
        byte[] ids = new byte[4096];
        byte[] metadata = new byte[2048];
        int source = 5 << 8 | 7 << 4 | 3;
        ids[source] = 35;
        metadata[source >> 1] = 0x10;
        NbtMap section = NbtMap.builder().putByteArray("Blocks", ids).putByteArray("Data", metadata).build();
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());

        BlockStorage[] layers = reader.readAnvilSection(section, 1);
        assertSame(BlockTypes.ORANGE_WOOL, layers[0].getBlock(CloudChunkSection.blockIndex(3, 5, 7)).getType());
        assertTrue(layers[1].isEmpty());
    }

    @Test
    void rejectsMalformedOrAmbiguousAnvilStorage() throws IOException {
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());
        NbtMap packed = NbtMap.builder().putIntArray("layer1", new int[4096]).putIntArray("layer2", new int[4096]).build();
        int[] invalidReferences = new int[4096];
        invalidReferences[0] = -1;

        assertThrows(IOException.class, () -> reader.readAnvilSection(packed, 5));
        assertThrows(IOException.class, () -> reader.readAnvilSection(packed, 1));
        assertThrows(IOException.class, () -> reader.readAnvilSection(packed.toBuilder().putIntArray("layer2", new int[1]).build(), 4));
        assertThrows(IOException.class, () -> reader.readAnvilSection(packed.toBuilder().putByteArray("Blocks", new byte[4096]).build(), 4));
        assertThrows(IOException.class, () -> reader.readAnvilSection(packed.toBuilder().putIntArray("layer1", invalidReferences).build(), 4));
    }

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
