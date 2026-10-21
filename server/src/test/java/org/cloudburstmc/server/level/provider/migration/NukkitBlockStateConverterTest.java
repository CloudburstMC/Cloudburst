package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NukkitBlockStateConverterTest {

    @BeforeAll
    static void initializeRegistry() {
        assertNotNull(BlockTestRegistry.get());
    }

    @Test
    void preservesNukkitIdsMetadataAndAxes() throws IOException {
        NukkitBlockStateConverter converter = new NukkitBlockStateConverter();

        assertSame(BlockTypes.AIR, converter.read(0, 0).getType());
        assertSame(BlockTypes.GRANITE, converter.read(1, 1).getType());
        assertSame(BlockTypes.SPRUCE_PLANKS, converter.read(5, 1).getType());
        assertSame(BlockTypes.SPRUCE_LOG, converter.read(17, 1).getType());
        assertSame(BlockTypes.ORANGE_WOOL, converter.read(35, 1).getType());

        BlockState sideways = converter.read(17, 8);

        assertSame(BlockTypes.OAK_LOG, sideways.getType());
        assertSame(Direction.Axis.Z, sideways.ensureTrait(BlockTraits.AXIS));
        assertThrows(IllegalArgumentException.class, () -> converter.read(65535, 0));
        assertThrows(IllegalArgumentException.class, () -> converter.read(17, -1));
    }

    @Test
    void convertsPackedMinecartDisplaysAndDoubleVectorsWithoutChangingSource() throws IOException {
        NukkitBlockStateConverter converter = new NukkitBlockStateConverter();
        NbtMap source = NbtMap.builder()
                .putString("id", "MinecartRideable")
                .putBoolean("CustomDisplayTile", true)
                .putInt("DisplayTile", 17 | 8 << 16)
                .putList("Pos", NbtType.DOUBLE, List.of(1.5, 64.0, -2.5))
                .putList("Motion", NbtType.DOUBLE, List.of(0.0, 0.25, 0.0))
                .build();

        NbtMap converted = converter.convertEntity(source);
        BlockState display = BlockStateCodec.read(converted.getCompound("DisplayBlock"));

        assertSame(BlockTypes.OAK_LOG, display.getType());
        assertSame(Direction.Axis.Z, display.ensureTrait(BlockTraits.AXIS));
        assertEquals(List.of(1.5f, 64.0f, -2.5f), converted.getList("Pos", NbtType.FLOAT));
        assertEquals(List.of(0.0f, 0.25f, 0.0f), converted.getList("Motion", NbtType.FLOAT));
        assertFalse(converted.containsKey("DisplayTile"));
        assertTrue(source.containsKey("DisplayTile"));
        assertFalse(source.containsKey("DisplayBlock"));
    }

    @Test
    void convertsFlowerPotAndMovingBlockDataBeforeLoading() throws IOException {
        NukkitBlockStateConverter converter = new NukkitBlockStateConverter();
        NbtMap pot = NbtMap.builder()
                .putString("id", "FlowerPot")
                .putShort("item", (short) 6)
                .putInt("data", 1)
                .build();
        NbtMap moving = NbtMap.builder()
                .putString("id", "MovingBlock")
                .putByte("movingBlockId", (byte) 17)
                .putByte("movingBlockData", (byte) 1)
                .putCompound("movingEntity", pot)
                .build();

        NbtMap convertedPot = converter.convertBlockEntity(pot);
        NbtMap convertedMoving = converter.convertBlockEntity(moving);

        assertSame(BlockTypes.SPRUCE_SAPLING,
                BlockStateCodec.read(convertedPot.getCompound("PlantBlock")).getType());
        assertFalse(convertedPot.containsKey("item"));
        assertFalse(convertedPot.containsKey("data"));
        assertSame(BlockTypes.SPRUCE_LOG,
                BlockStateCodec.read(convertedMoving.getCompound("movingBlock")).getType());
        assertEquals(convertedPot, convertedMoving.getCompound("movingEntity"));
        assertEquals((short) 6, pot.getShort("item"));
    }
}
