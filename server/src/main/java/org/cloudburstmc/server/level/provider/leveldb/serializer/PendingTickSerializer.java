package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.ChunkDataLoader;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.scheduler.BlockUpdateScheduler;
import org.cloudburstmc.server.utils.BlockUpdateEntry;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the {@code PENDING_TICKS} LevelDB record for a single chunk.
 */
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PendingTickSerializer {

    private static final String FIELD_CURRENT_TICK = "currentTick";
    private static final String FIELD_TICK_LIST = "tickList";
    private static final String FIELD_X = "x";
    private static final String FIELD_Y = "y";
    private static final String FIELD_Z = "z";
    private static final String FIELD_TIME = "time";
    private static final String FIELD_BLOCK_STATE = "blockState";

    /**
     * Reads the {@code PENDING_TICKS} record for the chunk described by
     * {@code builder} and registers a {@link ChunkDataLoader} that will
     * re-schedule the ticks once the chunk is fully built.
     */
    public static void loadPendingTicks(DB db, ChunkBuilder builder) {
        byte[] key = LevelDBKey.PENDING_TICKS.getKey(builder.getX(), builder.getZ());
        byte[] value = db.get(key);
        if (value == null) {
            return;
        }

        List<NbtMap> tickEntries = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(value);
             NBTInputStream in = NbtUtils.createReaderLE(bais)) {
            NbtMap root = (NbtMap) in.readTag();
            List<NbtMap> list = root.getList(FIELD_TICK_LIST, NbtType.COMPOUND);
            tickEntries.addAll(list);
        } catch (Exception e) {
            log.warn("Failed to load pending ticks for chunk ({}, {}): {}", builder.getX(), builder.getZ(), e.getMessage());
            return;
        }

        if (!tickEntries.isEmpty()) {
            builder.dataLoader(new PendingTickLoader(tickEntries));
        }
    }

    /**
     * Writes (or deletes) the {@code PENDING_TICKS} record for {@code chunk}
     * into {@code batch} and returns a {@link Runnable} that must be invoked
     * by the caller <em>after</em> the batch has been successfully committed
     * to disk. Calling the returned runnable records the save timestamp in
     * the scheduler so that {@link BlockUpdateScheduler#isDirty} returns the
     * correct value.
     *
     * <p>If there is no tick container registered for this chunk (e.g. the
     * chunk was never fully loaded), {@code null} is returned and nothing is
     * written to the batch.
     *
     * <p>If serialization fails, an error is logged, {@code null} is returned,
     * and the batch is not modified.
     *
     * @param batch the write batch to append to
     * @param chunk the chunk whose pending ticks should be persisted
     * @return a post-commit callback, or {@code null} if nothing was written
     */
    public static Runnable savePendingTicks(WriteBatch batch, CloudChunk chunk) {
        CloudLevel level = (CloudLevel) chunk.getLevel();
        BlockUpdateScheduler blockScheduler = level.getBlockUpdateQueue();
        BlockUpdateScheduler liquidScheduler = level.getLiquidUpdateQueue();
        long chunkKey = CloudChunk.key(chunk.getX(), chunk.getZ());

        List<BlockUpdateEntry> blockEntries = blockScheduler.packAll(chunkKey);
        List<BlockUpdateEntry> liquidEntries = liquidScheduler.packAll(chunkKey);
        if (blockEntries == null && liquidEntries == null) {
            return null;
        }

        List<BlockUpdateEntry> entries = new ArrayList<>((blockEntries == null ? 0 : blockEntries.size())
                + (liquidEntries == null ? 0 : liquidEntries.size()));
        if (blockEntries != null) {
            entries.addAll(blockEntries);
        }

        if (liquidEntries != null) {
            entries.addAll(liquidEntries);
        }

        byte[] dbKey = LevelDBKey.PENDING_TICKS.getKey(chunk.getX(), chunk.getZ());
        if (entries.isEmpty()) {
            batch.delete(dbKey);
            return () -> markSaved(blockScheduler, liquidScheduler, chunkKey);
        }

        List<NbtMap> tickList = new ArrayList<>(entries.size());
        for (BlockUpdateEntry entry : entries) {
            NbtMap blockState = BlockPalette.INSTANCE.getSerialized(entry.type.getDefaultState());
            NbtMap compound = NbtMap.builder()
                    .putInt(FIELD_X, entry.pos.getX())
                    .putInt(FIELD_Y, entry.pos.getY())
                    .putInt(FIELD_Z, entry.pos.getZ())
                    .putLong(FIELD_TIME, entry.delay)
                    .putCompound(FIELD_BLOCK_STATE, blockState)
                    .build();
            tickList.add(compound);
        }

        NbtMap root = NbtMap.builder()
                .putInt(FIELD_CURRENT_TICK, 0)
                .putList(FIELD_TICK_LIST, NbtType.COMPOUND, tickList)
                .build();

        byte[] value;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             NBTOutputStream out = NbtUtils.createWriterLE(baos)) {
            out.writeTag(root);
            value = baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to serialize pending ticks for chunk ({}, {}): {}", chunk.getX(), chunk.getZ(), e.getMessage());
            return null;
        }

        batch.put(dbKey, value);
        return () -> markSaved(blockScheduler, liquidScheduler, chunkKey);
    }

    private record PendingTickLoader(List<NbtMap> tickEntries) implements ChunkDataLoader {
        @Override
        public boolean load(CloudChunk chunk) {
            CloudLevel level = (CloudLevel) chunk.getLevel();

            // Accept only ticks within the chunk's own column and within
            // the level's vertical build range. Entries outside this range
            // are corrupt or stale upgrade data and are discarded with a warning.
            int minX = chunk.getX() << 4;
            int maxX = minX + 15;
            int minZ = chunk.getZ() << 4;
            int maxZ = minZ + 15;
            int minY = level.getMinHeight();
            int maxY = level.getMaxHeight();

            List<BlockUpdateEntry> restored = new ArrayList<>(tickEntries.size());
            for (NbtMap entry : tickEntries) {
                try {
                    int x = entry.getInt(FIELD_X);
                    int y = entry.getInt(FIELD_Y);
                    int z = entry.getInt(FIELD_Z);
                    long time = entry.getLong(FIELD_TIME);

                    if (x < minX || x > maxX || z < minZ || z > maxZ || y < minY || y > maxY) {
                        log.warn("Discarding out-of-bounds pending tick at ({}, {}, {}) for chunk ({}, {})", x, y, z, chunk.getX(), chunk.getZ());
                        continue;
                    }

                    Vector3i pos = Vector3i.from(x, y, z);
                    NbtMap serializedState = entry.getCompound(FIELD_BLOCK_STATE);
                    BlockState state = BlockPalette.INSTANCE.getSerializedPalette().get(serializedState);
                    if (state == null) {
                        log.warn("Discarding pending tick for unknown serialized block state {} at ({}, {}, {}) "
                                        + "in level \"{}\" chunk ({}, {})",
                                serializedState, x, y, z, level.getName(), chunk.getX(), chunk.getZ());
                        continue;
                    }
                    restored.add(BlockUpdateEntry.ofRestored(pos, state.getType(), time));
                } catch (Exception e) {
                    log.warn("Skipping malformed pending tick entry: {}", e.getMessage());
                }
            }

            if (!restored.isEmpty()) {
                chunk.setRestoredTicks(restored);
            }

            return false;
        }
    }

    private static void markSaved(BlockUpdateScheduler blockScheduler, BlockUpdateScheduler liquidScheduler,
                                  long chunkKey) {
        blockScheduler.markSaved(chunkKey);
        liquidScheduler.markSaved(chunkKey);
    }
}
