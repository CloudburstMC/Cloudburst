package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BedrockLevelDBChunkCodecTest {

    @Test
    void readsNamedLayersWithoutNumericMappings() throws IOException {
        BlockStorage primary = new BlockStorage();
        primary.setBlock(4095, BlockTestRegistry.get().getBlock(BlockTypes.STONE));
        BlockStorage secondary = new BlockStorage();
        secondary.setBlock(4095, BlockTestRegistry.get().getBlock(BlockTypes.WATER));
        ByteBuf buffer = Unpooled.buffer();
        byte[] data;
        try {
            buffer.writeByte(9).writeByte(2).writeByte(-4);
            primary.writeToStorage(buffer);
            secondary.writeToStorage(buffer);
            data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
        } finally {
            buffer.release();
        }

        BedrockLevelDBChunkCodec codec = new BedrockLevelDBChunkCodec();
        BlockStorage[] restored = codec.readSection(data, -4);

        assertSame(BlockTypes.STONE, restored[0].getBlock(4095).getType());
        assertSame(BlockTypes.WATER, restored[1].getBlock(4095).getType());
        assertSame(BlockTypes.AIR, restored[0].getBlock(0).getType());
        assertThrows(IOException.class, () -> codec.readSection(data, 0));
        assertThrows(IOException.class, () -> codec.readSection(Arrays.copyOf(data, data.length - 1), -4));
    }

    @Test
    void unregisteredBedrockBlocksBecomeVisibleUnknownBlocksInTerrainAndTicks() throws IOException {
        CloudBlockRegistry registry = BlockTestRegistry.get();
        NbtMap custom = BlockStateCodec.write(registry.getBlock(BlockTypes.STONE)).toBuilder()
                .putString("name", "test:unregistered_block").build();
        BedrockLevelDBChunkCodec codec = new BedrockLevelDBChunkCodec();
        ByteBuf buffer = Unpooled.buffer();
        byte[] data;
        try {
            buffer.writeByte(9).writeByte(2).writeByte(4);
            try (NBTOutputStream output = NbtUtils.createWriterLE(new ByteBufOutputStream(buffer))) {
                buffer.writeByte(0);
                output.writeTag(custom);
                buffer.writeByte(0);
                output.writeTag(BlockStateCodec.write(BlockTypes.WATER.getDefaultState()));
            }

            data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
        } finally {
            buffer.release();
        }

        BlockStorage[] restored = codec.readSection(data, 4);

        assertSame(BlockTypes.UNKNOWN.getDefaultState(), restored[0].getBlock(0));
        assertSame(BlockTypes.UNKNOWN.getDefaultState(), restored[0].getBlock(4095));
        assertSame(BlockTypes.WATER.getDefaultState(), restored[1].getBlock(0));
        assertSame(BlockTypes.UNKNOWN.getDefaultState(), codec.readTickState(NbtMap.builder().putCompound("blockState", custom).build()));
        assertThrows(IllegalArgumentException.class, () -> BlockStateCodec.read(custom));

        ByteBuf persisted = Unpooled.buffer();
        try {
            restored[0].writeToStorage(persisted);
            assertSame(BlockTypes.UNKNOWN.getDefaultState(), BlockStorage.readFromStorage(persisted).getBlock(0));
        } finally {
            persisted.release();
        }
    }

    @Test
    void completedBedrockTerrainDoesNotRunNukkitPopulationRules() throws IOException {
        BedrockLevelDBChunkCodec codec = new BedrockLevelDBChunkCodec();
        for (int state : new int[]{0, 2, 3}) {
            assertSame(ChunkGenerationStatus.FINISHED, codec.readGeneration(finalizationState(state)));
        }

        assertSame(ChunkGenerationStatus.GENERATED, codec.readGeneration(finalizationState(1)));
        assertThrows(IOException.class, () -> codec.readGeneration(finalizationState(4)));
        assertThrows(IOException.class, () -> codec.readGeneration(new byte[3]));
    }

    @Test
    void rejectsNumericReferencesInsteadOfApplyingNukkitIds() {
        BedrockLevelDBChunkCodec codec = new BedrockLevelDBChunkCodec();
        NbtMap item = NbtMap.builder().putShort("id", (short) 35).putByte("Count", (byte) 1).build();
        NbtMap chest = NbtMap.builder().putString("id", "Chest").putCompound("Item", item).build();

        assertThrows(IOException.class, () -> codec.readLegacyBlock(35, 1));
        assertThrows(IOException.class, () -> codec.readSection(new byte[]{0}, 0));
        assertThrows(IllegalArgumentException.class, () -> codec.convertBlockEntity(chest));
    }

    @Test
    void normalizesEmptyBedrockSlotsWithoutLosingSlotDataOrNamedItems() {
        BedrockLevelDBChunkCodec codec = new BedrockLevelDBChunkCodec();
        NbtMap empty = NbtMap.builder().putString("Name", "").putByte("Count", (byte) 0)
                .putShort("Damage", (short) 0).putByte("Slot", (byte) 2).build();
        NbtMap occupied = NbtMap.builder().putString("Name", "minecraft:stone").putByte("Count", (byte) 3)
                .putByte("Slot", (byte) 4).build();
        NbtMap boat = NbtMap.builder().putString("identifier", "minecraft:chest_boat")
                .putList("ChestItems", NbtType.COMPOUND, List.of(empty, occupied)).build();
        NbtMap expected = empty.toBuilder().putString("Name", "minecraft:air").build();

        assertEquals(List.of(expected, occupied), codec.convertEntity(boat).getList("ChestItems", NbtType.COMPOUND));
        assertEquals("", empty.getString("Name"));

        NbtMap chest = NbtMap.builder().putString("id", "Chest").putCompound("Item", empty).build();

        assertEquals(expected, codec.convertBlockEntity(chest).getCompound("Item"));
    }

    @Test
    void rejectsInvalidNonemptyItemNames() {
        BedrockLevelDBChunkCodec codec = new BedrockLevelDBChunkCodec();
        for (NbtMap item : List.of(
                NbtMap.builder().putString("Name", "").putByte("Count", (byte) 1).build(),
                NbtMap.builder().putString("Name", " ").putByte("Count", (byte) 0).build(),
                NbtMap.builder().putInt("Name", 1).putByte("Count", (byte) 0).build())) {
            NbtMap entity = NbtMap.builder().putCompound("Item", item).build();

            assertThrows(IllegalArgumentException.class, () -> codec.convertEntity(entity));
        }
    }

    @Test
    void biomeColumnsAndHeightOriginsRemainEncodingSpecific() throws IOException {
        byte[] biomes = new byte[256];
        biomes[2 * 16 + 7] = 12;
        biomes[7 * 16 + 2] = 4;
        LevelDBChunkCodec bedrock = new BedrockLevelDBChunkCodec();
        LevelDBChunkCodec nukkit = new NukkitLevelDBChunkCodec();

        assertEquals(12, nukkit.readColumnBiome(biomes, 2, 7));
        assertEquals(4, bedrock.readColumnBiome(biomes, 2, 7));
        assertEquals(128, nukkit.readHeight(128, -64, true));
        assertEquals(64, bedrock.readHeight(128, -64, true));
        assertEquals(128, bedrock.readHeight(128, -64, false));
        assertEquals(-1, bedrock.readHeight(65535, -64, false));
    }

    private static byte[] finalizationState(int state) {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(state).array();
    }
}
