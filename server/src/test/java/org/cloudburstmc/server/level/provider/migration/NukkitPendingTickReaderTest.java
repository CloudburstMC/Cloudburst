package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.iq80.leveldb.DB;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NukkitPendingTickReaderTest {

    private static final NbtMap STONE = BlockStateCodec.write(BlockTestRegistry.get().getBlock(BlockTypes.STONE));

    @Test
    void rehomesWestNorthAndCornerTicksWithoutDuplicatingThem() throws Exception {
        Records records = new Records();
        records.save(0, 0, 0, 0,
                tick(0, 64, 0, 12), tick(-1, 64, 0, 13),
                tick(0, 64, -1, 14), tick(-2, 64, -2, 15));
        NukkitPendingTickReader reader = records.reader(0);

        for (Vector3i position : List.of(Vector3i.from(0, 64, 0), Vector3i.from(-1, 64, 0),
                Vector3i.from(0, 64, -1), Vector3i.from(-2, 64, -2))) {
            List<NbtMap> ticks = reader.read(position.getX() >> 4, position.getZ() >> 4, -64, 320)
                    .getList("tickList", NbtType.COMPOUND);

            assertEquals(1, ticks.size());
            NbtMap restored = ticks.getFirst();
            assertEquals(position.getX(), restored.getInt("x"));
            assertEquals(position.getZ(), restored.getInt("z"));
            assertEquals(STONE, restored.getCompound("blockState"));
        }
    }

    @Test
    void prefersTheOwningChunkWhenNeighborSnapshotsDisagree() throws Exception {
        Records records = new Records();
        records.save(0, 0, 0, 100, tick(15, 64, 15, 130).toBuilder().putInt("p", 3).build());
        records.save(1, 1, 0, 0, tick(15, 64, 15, 8).toBuilder().putInt("p", 1).build());

        NbtMap root = records.reader(0).read(0, 0, -64, 320);
        List<NbtMap> ticks = root.getList("tickList", NbtType.COMPOUND);

        assertEquals(0, root.getInt("currentTick"));
        assertEquals(1, ticks.size());
        assertEquals(30, ticks.getFirst().getLong("time"));
        assertEquals(3, ticks.getFirst().getInt("p"));
    }

    @Test
    void preservesNeighborOnlyTicksAndUsesTheEarliestDuplicate() throws Exception {
        Records records = new Records();
        records.save(1, 0, 0, 0, tick(15, 64, 15, 30));
        records.save(1, 1, 0, 0, tick(15, 64, 15, 20));

        List<NbtMap> ticks = records.reader(0).read(0, 0, -64, 320).getList("tickList", NbtType.COMPOUND);

        assertEquals(1, ticks.size());
        assertEquals(20, ticks.getFirst().getLong("time"));
    }

    @Test
    void supportsNumericReferencesAndClampsExpiredDelays() throws Exception {
        Records records = new Records();
        NbtMap numeric = NbtMap.builder()
                .putInt("x", 1).putInt("y", 64).putInt("z", 1)
                .putCompound("blockState", NbtMap.EMPTY).putByte("tileID", (byte) 1)
                .putLong("time", 90).build();
        records.save(0, 0, 0, 100, numeric);

        List<NbtMap> ticks = records.reader(0).read(0, 0, -64, 320).getList("tickList", NbtType.COMPOUND);

        assertEquals(1, ticks.size());
        assertEquals(0, ticks.getFirst().getLong("time"));
        assertEquals(STONE, ticks.getFirst().getCompound("blockState"));
        assertFalse(ticks.getFirst().containsKey("tileID"));
    }

    @Test
    void rejectsGenuineOutOfBoundsTicksWithTheirSourceChunkInTheError() throws Exception {
        Records records = new Records();
        records.save(0, 0, 0, 0, tick(16, 64, 0, 5));
        NukkitPendingTickReader reader = records.reader(0);

        IOException failure = assertThrows(IOException.class, () -> reader.read(0, 0, -64, 320));
        assertTrue(failure.getMessage().contains("source chunk (0, 0)"));

        records.save(0, 0, 0, 0, tick(0, 320, 0, 5));
        assertThrows(IOException.class, () -> reader.read(0, 0, -64, 320));
    }

    @Test
    void keepsDimensionsSeparate() throws Exception {
        Records records = new Records();
        records.save(0, 0, 0, 0, tick(0, 64, 0, 5));

        assertTrue(records.reader(1).read(0, 0, 0, 128).isEmpty());

        records.save(0, 0, 1, 0, tick(0, 64, 0, 7));
        List<NbtMap> ticks = records.reader(1).read(0, 0, 0, 128).getList("tickList", NbtType.COMPOUND);
        assertEquals(1, ticks.size());
        assertEquals(7, ticks.getFirst().getLong("time"));
    }

    private static NbtMap tick(int x, int y, int z, long time) {
        return NbtMap.builder().putInt("x", x).putInt("y", y).putInt("z", z)
                .putCompound("blockState", STONE).putLong("time", time).build();
    }

    private static class Records {

        private final Map<ByteBuffer, byte[]> data = new HashMap<>();

        private void save(int x, int z, int dimension, int currentTick, NbtMap... ticks) throws IOException {
            NbtMap root = NbtMap.builder().putInt("currentTick", currentTick)
                    .putList("tickList", NbtType.COMPOUND, List.of(ticks)).build();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (NBTOutputStream output = NbtUtils.createWriterLE(bytes)) {
                output.writeTag(root);
            }

            this.data.put(ByteBuffer.wrap(NukkitLevelDBKeys.key(x, z, dimension, '3')), bytes.toByteArray());
        }

        private NukkitPendingTickReader reader(int dimension) throws IOException {
            return new NukkitPendingTickReader(InterfaceProxy.create(DB.class, (proxy, method, arguments) -> {
                if (method.getName().equals("get")) {
                    return this.data.get(ByteBuffer.wrap((byte[]) arguments[0]));
                }

                throw new AssertionError("Unexpected database access: " + method);
            }), dimension, new NukkitBlockStateConverter());
        }
    }
}
