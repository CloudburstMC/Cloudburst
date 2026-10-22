package org.cloudburstmc.server.level.provider.anvil;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.provider.migration.NukkitBlockStateConverter;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NukkitAnvilPendingTicksTest {

    @BeforeAll
    static void initializeRegistry() {
        assertNotNull(BlockTestRegistry.get());
    }

    @Test
    void rehomesBorderCopiesAndPrefersTheOwningSnapshot() throws IOException {
        NukkitBlockStateConverter blocks = new NukkitBlockStateConverter();
        NukkitAnvilPendingTicks ticks = new NukkitAnvilPendingTicks(blocks);
        ticks.add(1, -18, records(tick(20, 1)));
        ticks.add(0, -18, records(tick(3, 2)));

        NbtMap restored = ticks.take(0, -18, -64, 320, terrain(10, 2));
        List<NbtMap> entries = restored.getList("tickList", NbtType.COMPOUND);

        assertEquals(1, entries.size());
        assertEquals(0, restored.getInt("currentTick"));
        assertEquals(3, entries.getFirst().getLong("time"));
        assertEquals(2, entries.getFirst().getInt("p"));
        assertEquals(BlockStateCodec.write(blocks.read(10, 2)), entries.getFirst().getCompound("blockState"));
        ticks.requireConsumed();
    }

    @Test
    void retainsNeighborOnlyTicksAndClampsExpiredDelays() throws IOException {
        NukkitAnvilPendingTicks ticks = new NukkitAnvilPendingTicks(new NukkitBlockStateConverter());
        ticks.add(1, -18, records(tick(-3, 4)));

        List<NbtMap> restored = ticks.take(0, -18, -64, 320, terrain(10, 0)).getList("tickList", NbtType.COMPOUND);

        assertEquals(1, restored.size());
        assertEquals(0, restored.getFirst().getLong("time"));
        assertEquals(4, restored.getFirst().getInt("p"));
        ticks.requireConsumed();
    }

    @Test
    void ignoresTicksWhoseBlockWasReplaced() throws IOException {
        NukkitAnvilPendingTicks ticks = new NukkitAnvilPendingTicks(new NukkitBlockStateConverter());
        ticks.add(0, -18, records(tick(3, 0)));

        assertEquals(NbtMap.EMPTY, ticks.take(0, -18, -64, 320, terrain(1, 0)));
        ticks.requireConsumed();
    }

    @Test
    void rejectsUnknownSaveIdsInvalidBordersAndMissingDestinations() throws IOException {
        NukkitAnvilPendingTicks ticks = new NukkitAnvilPendingTicks(new NukkitBlockStateConverter());
        NbtMap valid = tick(3, 0);

        assertThrows(IOException.class, () -> ticks.add(0, -18, records(valid.toBuilder().putString("i", "UnknownBlockClass").build())));
        assertThrows(IOException.class, () -> ticks.add(2, -18, records(valid)));

        ticks.add(0, -18, records(valid));
        assertThrows(IOException.class, ticks::requireConsumed);
        assertThrows(IOException.class, () -> ticks.take(0, -18, 64, 320, terrain(10, 0)));
    }

    @Test
    void resolvesSaveIdsThroughThePinnedBlockMappings() throws IOException {
        NukkitBlockStateConverter blocks = new NukkitBlockStateConverter();

        assertEquals(10, blocks.readSaveId("BlockLava"));
        assertEquals(8, blocks.readSaveId("BlockWater"));
        assertSame(BlockTypes.STONE, blocks.read(blocks.readSaveId("BlockStone"), 0).getType());
        assertThrows(IllegalArgumentException.class, () -> blocks.readSaveId("MissingBlock"));
    }

    private static NbtMap tick(int delay, int priority) {
        return NbtMap.builder().putInt("x", 15).putInt("y", 36).putInt("z", -285)
                .putString("i", "BlockLava").putInt("t", delay).putInt("p", priority).build();
    }

    private static NbtMap records(NbtMap tick) {
        return NbtMap.builder().putList("TileTicks", NbtType.COMPOUND, List.of(tick)).build();
    }

    private static NbtMap terrain(int id, int metadata) {
        int[] primary = new int[4096];
        primary[15 << 8 | 3 << 4 | 4] = id << 6 | metadata;
        NbtMap section = NbtMap.builder().putByte("Y", (byte) 2)
                .putIntArray("layer1", primary).putIntArray("layer2", new int[4096]).build();
        return NbtMap.builder().putByte("V", (byte) 4)
                .putList("Sections", NbtType.COMPOUND, List.of(section)).build();
    }
}
