package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockLayer;
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
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class NukkitBlockStorageReaderTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void restoresAnvilExtraDataWithItsOwnCoordinatesAndByteOrder() throws IOException {
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());
        CloudChunkSection[] sections = new CloudChunkSection[24];
        int key = 13 << 13 | 2 << 9 | (-12 + 64);

        reader.readAnvilExtraData(extraData(key, 1 << 8 | 35), sections, -4, REGISTRY);

        CloudChunkSection restored = assertInstanceOf(CloudChunkSection.class, sections[3]);
        assertSame(BlockTypes.ORANGE_WOOL, restored.getBlockState(13, 4, 2, BlockLayer.SECONDARY).getType());
        assertSame(BlockTypes.AIR, restored.getBlockState(13, 4, 2, BlockLayer.PRIMARY).getType());
    }

    @Test
    void anvilExtraDataDoesNotOverwriteConflictingSecondaryBlocks() throws IOException {
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());
        CloudChunkSection[] sections = new CloudChunkSection[24];
        int key = 3 << 13 | 5 << 9 | (36 + 64);
        byte[] water = extraData(key, 9);
        sections[6] = new CloudChunkSection(REGISTRY);
        sections[6].setBlockState(3, 4, 5, BlockLayer.PRIMARY, BlockTypes.STONE.getDefaultState());

        reader.readAnvilExtraData(water, sections, -4, REGISTRY);
        reader.readAnvilExtraData(water, sections, -4, REGISTRY);

        assertThrows(IOException.class, () -> reader.readAnvilExtraData(extraData(key, 10), sections, -4, REGISTRY));
        CloudChunkSection restored = assertInstanceOf(CloudChunkSection.class, sections[6]);
        assertSame(BlockTypes.WATER, restored.getBlockState(3, 4, 5, BlockLayer.SECONDARY).getType());
        assertSame(BlockTypes.STONE, restored.getBlockState(3, 4, 5, BlockLayer.PRIMARY).getType());
    }

    @Test
    void validatesAnvilExtraDataBeforeLosingSavedState() throws IOException {
        NukkitBlockStorageReader reader = new NukkitBlockStorageReader(new NukkitBlockStateConverter());
        CloudChunkSection[] sections = new CloudChunkSection[16];

        reader.readAnvilExtraData(new byte[0], sections, 0, REGISTRY);
        reader.readAnvilExtraData(new byte[4], sections, 0, REGISTRY);

        assertThrows(IOException.class, () -> reader.readAnvilExtraData(new byte[3], sections, 0, REGISTRY));
        assertThrows(IOException.class, () -> reader.readAnvilExtraData(new byte[]{0, 0, 0, 1}, sections, 0, REGISTRY));
        assertThrows(IOException.class, () -> reader.readAnvilExtraData(extraData(1 << 17, 9), sections, 0, REGISTRY));
        assertThrows(IOException.class, () -> reader.readAnvilExtraData(extraData(300 + 64, 9), sections, 0, REGISTRY));
        assertThrows(IOException.class, () -> reader.readAnvilExtraData(extraData(64, 0xffff), sections, 0, REGISTRY));
    }

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

    private static byte[] extraData(int key, int value) {
        return ByteBuffer.allocate(10).putInt(1).putInt(key).putShort((short) value).array();
    }

    private static void writeSingleton(ByteBuf buffer, NbtMap state) throws IOException {
        buffer.writeByte(0);
        try (NBTOutputStream output = NbtUtils.createWriterLE(new ByteBufOutputStream(buffer))) {
            output.writeTag(state);
        }
    }
}
