package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.iq80.leveldb.DB;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts Nukkit's overlapping saved tick lists into chunk-local pending ticks.
 */
public class NukkitPendingTickReader {

    private final DB database;
    private final int dimension;
    private final NukkitBlockStateConverter blocks;

    public NukkitPendingTickReader(DB database, int dimension, NukkitBlockStateConverter blocks) {
        this.database = Objects.requireNonNull(database, "database");
        this.dimension = dimension;
        this.blocks = Objects.requireNonNull(blocks, "blocks");
    }

    public NbtMap read(int chunkX, int chunkZ, int minY, int maxY) throws IOException {
        Map<TickKey, ImportedTick> ticks = new LinkedHashMap<>();

        for (int offsetX = 0; offsetX <= 1; offsetX++) {
            for (int offsetZ = 0; offsetZ <= 1; offsetZ++) {
                int sourceX = Math.addExact(chunkX, offsetX);
                int sourceZ = Math.addExact(chunkZ, offsetZ);
                byte[] data = this.database.get(NukkitLevelDBKeys.key(sourceX, sourceZ, this.dimension, '3'));
                if (data == null) {
                    continue;
                }

                try {
                    this.readRecord(data, sourceX, sourceZ, chunkX, chunkZ, minY, maxY, ticks);
                } catch (IOException | RuntimeException failure) {
                    throw new IOException("Invalid Nukkit pending ticks in source chunk (" + sourceX + ", " + sourceZ + ") while importing chunk (" + chunkX + ", " + chunkZ + ")", failure);
                }
            }
        }

        if (ticks.isEmpty()) {
            return NbtMap.EMPTY;
        }

        List<NbtMap> entries = ticks.values().stream().map(ImportedTick::tag).toList();
        return NbtMap.builder().putInt("currentTick", 0).putList("tickList", NbtType.COMPOUND, entries).build();
    }

    private void readRecord(byte[] data, int sourceX, int sourceZ, int chunkX, int chunkZ, int minY, int maxY, Map<TickKey, ImportedTick> ticks) throws IOException {
        NbtMap root = readRoot(data);
        if (!root.containsKey("currentTick", NbtType.INT) || !(root.get("tickList") instanceof NbtList<?> entries) || entries.getType() != NbtType.COMPOUND) {
            throw new IOException("Pending ticks require an integer currentTick and a compound tickList");
        }

        for (Object entry : entries) {
            NbtMap tag = (NbtMap) entry;
            if (!tag.containsKey("x", NbtType.INT) || !tag.containsKey("y", NbtType.INT) || !tag.containsKey("z", NbtType.INT)) {
                throw new IOException("Pending tick has invalid coordinates: " + tag);
            }

            Vector3i position = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            long minX = (long) sourceX << 4;
            long minZ = (long) sourceZ << 4;
            if (position.getX() < minX - 2 || position.getX() >= minX + 16 || position.getZ() < minZ - 2 || position.getZ() >= minZ + 16) {
                throw new IOException("Pending tick is outside Nukkit's saved chunk border: " + position);
            }

            if ((position.getX() >> 4) != chunkX || (position.getZ() >> 4) != chunkZ) {
                continue;
            }

            if (position.getY() < minY || position.getY() >= maxY) {
                throw new IOException("Pending tick is outside the destination height: " + position);
            }

            if (!tag.containsKey("time", NbtType.LONG)) {
                throw new IOException("Pending tick has no long delay: " + position);
            }

            NbtMap serialized = tag.getCompound("blockState");
            BlockState state;
            if (serialized.containsKey("name")) {
                state = this.blocks.readNamed(serialized);
            } else if (tag.containsKey("tileID", NbtType.BYTE)) {
                state = this.blocks.read(tag.getByte("tileID") & 0xff, 0);
            } else {
                throw new IOException("Pending tick has no block reference: " + position);
            }

            long delay = Math.max(0, Math.subtractExact(tag.getLong("time"), root.getInt("currentTick")));
            NbtMap converted = NbtMap.builder()
                    .putInt("x", position.getX()).putInt("y", position.getY()).putInt("z", position.getZ())
                    .putCompound("blockState", BlockStateCodec.write(state))
                    .putLong("time", delay).putInt("p", tag.getInt("p")).build();
            ImportedTick tick = new ImportedTick(converted, sourceX == chunkX && sourceZ == chunkZ);
            ticks.merge(new TickKey(position, state.getType()), tick, NukkitPendingTickReader::selectTick);
        }
    }

    private static ImportedTick selectTick(ImportedTick first, ImportedTick second) {
        if (first.owned() != second.owned()) {
            return first.owned() ? first : second;
        }

        int order = Long.compare(first.tag().getLong("time"), second.tag().getLong("time"));
        if (order == 0) {
            order = Integer.compare(first.tag().getInt("p"), second.tag().getInt("p"));
        }

        return order <= 0 ? first : second;
    }

    private static NbtMap readRoot(byte[] data) throws IOException {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             NBTInputStream input = NbtUtils.createReaderLE(bytes)) {
            if (!(input.readTag() instanceof NbtMap root) || bytes.available() != 0) {
                throw new IOException("Pending ticks must contain one root compound");
            }
            return root;
        }
    }

    private record TickKey(Vector3i position, BlockType type) {}

    private record ImportedTick(NbtMap tag, boolean owned) {}
}
