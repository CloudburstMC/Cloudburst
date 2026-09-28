package org.cloudburstmc.server.level.chunk;

import co.aikar.timings.Timing;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.ChunkSection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.scheduler.BlockUpdateScheduler;
import org.cloudburstmc.server.utils.BlockUpdateEntry;

import java.io.Closeable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

@Log4j2
public final class CloudChunk implements Chunk, Closeable {

    static final int ARRAY_SIZE = 256;
    private static final BiomeStorage defaultBiomeStorage = new BiomeStorage(CloudChunkSection.DEFAULT_BIOME_ID);

    private final Lock readLock; // cached from ReadWriteLock to avoid interface dispatch overhead
    private final Lock writeLock;

    private final UnsafeChunk unsafe;
    private List<CloudChunkLoadTask> loadTasks = List.of();

    /**
     * Pending tick entries that were deserialized from disk during
     * {@link #initialize()} but could not be scheduled yet because the tick
     * container is not registered until {@link #replayRestoredTicks()} is
     * called (after the chunk finishes loading and its container is wired).
     */
    private List<BlockUpdateEntry> restoredTicks;

    public CloudChunk(int x, int z, Level level) {
        this(new UnsafeChunk(x, z, level));
    }

    CloudChunk(UnsafeChunk unsafe, List<CloudChunkLoadTask> loadTasks) {
        this(unsafe);
        this.loadTasks = List.copyOf(checkNotNull(loadTasks, "loadTasks"));
    }

    private CloudChunk(@NonNull UnsafeChunk unsafe) {
        this.unsafe = unsafe;

        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();

    }

    /**
     * Applies data that could not be restored until the chunk was constructed.
     */
    public void initialize() {
        if (this.unsafe.beginInitialization()) {
            try (Timing ignored = ((CloudLevel) unsafe.getLevel()).timings.syncChunkLoadEntitiesTimer.startTiming()) {
                List<CloudChunkLoadTask> loadTasks = this.loadTasks;
                this.loadTasks = List.of();
                for (CloudChunkLoadTask loadTask : loadTasks) {
                    if (loadTask.load(this)) {
                        this.markDirty();
                    }
                }

                this.unsafe.completeInitialization();
            }
        }
    }

    /**
     * Stores tick entries that were deserialized from disk. Called by the
     * pending-tick load task during {@link #initialize()} so entries can be replayed
     * later, once the tick container is registered.
     *
     * <p>Replaces any previously stored list; only one call per chunk load.
     */
    public void setRestoredTicks(List<BlockUpdateEntry> entries) {
        this.restoredTicks = entries;
    }

    /**
     * Replays restored pending ticks into the scheduler.
     *
     * <p>Must be called only after the tick container for this chunk has been
     * registered via {@code BlockUpdateScheduler.registerTickContainer}.
     * Calling it earlier will silently drop entries.
     *
     * <p>This method is idempotent: it clears the restored ticks after replaying
     * them so a second call is a no-op.
     */
    public void replayRestoredTicks() {
        CloudLevel lvl = (CloudLevel) this.unsafe.getLevel();

        if (this.restoredTicks != null) {
            for (BlockUpdateEntry entry : this.restoredTicks) {
                BlockUpdateScheduler scheduler = entry.type.isLiquid() ? lvl.getLiquidUpdateQueue() : lvl.getBlockUpdateQueue();
                scheduler.add(entry);
            }
            this.restoredTicks = null;
        }
    }

    @NonNull
    @Override
    public ChunkSection getOrCreateSection(int y) {
        this.writeLock.lock();
        try {
            return unsafe.getOrCreateSection(y);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Nullable
    @Override
    public ChunkSection getSection(int y) {
        this.readLock.lock();
        try {
            return unsafe.getSection(y);
        } finally {
            this.readLock.unlock();
        }
    }

    @NonNull
    @Override
    public ChunkSection[] getSections() {
        this.readLock.lock();
        try {
            return this.unsafe.getSections();
        } finally {
            this.readLock.unlock();
        }
    }

    @NonNull
    @Override
    public BlockState getBlockState(int x, int y, int z, BlockLayer layer) {
        this.readLock.lock();
        try {
            return unsafe.getBlockState(x, y, z, layer);
        } finally {
            this.readLock.unlock();
        }
    }

    @NonNull
    @Override
    public BlockState setBlockState(int x, int y, int z, BlockLayer layer, BlockState blockState) {
        this.writeLock.lock();
        try {
            return unsafe.setBlockState(x, y, z, layer, blockState);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getBiome(int x, int y, int z) {
        this.readLock.lock();
        try {
            return unsafe.getBiome(x, y, z);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void setBiome(int x, int y, int z, int biome) {
        this.writeLock.lock();
        try {
            unsafe.setBiome(x, y, z, biome);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void fillColumnBiome(int x, int z, int biomeId) {
        this.writeLock.lock();
        try {
            this.unsafe.fillColumnBiome(x, z, biomeId);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        this.readLock.lock();
        try {
            return unsafe.getSkyLight(x, y, z);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void setSkyLight(int x, int y, int z, int level) {
        this.writeLock.lock();
        try {
            unsafe.setSkyLight(x, y, z, level);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        this.readLock.lock();
        try {
            return unsafe.getBlockLight(x, y, z);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void setBlockLight(int x, int y, int z, int level) {
        this.writeLock.lock();
        try {
            unsafe.setBlockLight(x, y, z, level);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getHighestBlock(int x, int z) {
        this.readLock.lock();
        try {
            return this.unsafe.getHighestBlock(x, z);
        } finally {
            this.readLock.unlock();
        }
    }

    public void registerEntity(@NonNull Entity entity) {
        this.writeLock.lock();
        try {
            unsafe.registerEntity(entity);
        } finally {
            this.writeLock.unlock();
        }
    }

    public void unregisterEntity(Entity entity) {
        this.writeLock.lock();
        try {
            unsafe.unregisterEntity(entity);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public int getX() {
        return unsafe.getX();
    }

    @Override
    public int getZ() {
        return unsafe.getZ();
    }

    @NonNull
    @Override
    public Level getLevel() {
        return unsafe.getLevel();
    }

    @Override
    public int @NonNull [] getHeightMap() {
        this.readLock.lock();
        try {
            return this.unsafe.getHeightMap();
        } finally {
            this.readLock.unlock();
        }
    }

    @NonNull
    @Override
    public Set<CloudPlayer> getPlayers() {
        this.readLock.lock();
        try {
            return this.unsafe.getPlayers();
        } finally {
            this.readLock.unlock();
        }
    }

    @NonNull
    @Override
    public Set<CloudEntity> getEntities() {
        this.readLock.lock();
        try {
            return this.unsafe.getEntities();
        } finally {
            this.readLock.unlock();
        }
    }

    public ChunkGenerationStatus getGenerationStatus() {
        return this.unsafe.getGenerationStatus();
    }

    public void advanceGenerationStatus(ChunkGenerationStatus nextStatus) {
        this.unsafe.advanceGenerationStatus(nextStatus);
    }

    @Override
    public boolean isGenerated() {
        return this.unsafe.isGenerated();
    }

    public boolean isPopulated() {
        return this.unsafe.isPopulated();
    }

    public boolean isFinished() {
        return this.unsafe.isFinished();
    }

    public boolean isDirty() {
        return this.unsafe.isDirty();
    }

    public void markDirty() {
        this.unsafe.markDirty();
    }

    /**
     * Captures the content revision represented by a serialized chunk snapshot.
     *
     * @return the current content revision
     */
    public long captureSaveRevision() {
        return this.unsafe.captureSaveRevision();
    }

    /**
     * Acknowledges that a serialized revision was persisted successfully.
     * Changes made after that revision keep the chunk dirty.
     *
     * @param saveRevision the revision captured while serializing
     */
    public void acknowledgeSave(long saveRevision) {
        this.unsafe.acknowledgeSave(saveRevision);
    }

    @NonNull
    @Override
    public Set<CloudPlayer> getViewers() {
        return this.unsafe.getViewers();
    }

    /**
     * Acquires a direct chunk view under this chunk's read lock.
     *
     * <p>The returned view must be closed by the acquiring thread.
     *
     * @return the acquired chunk view
     */
    public LockedChunk lockForRead() {
        return new LockedChunk(this.unsafe, this.readLock, false);
    }

    /**
     * Acquires a direct chunk view under this chunk's write lock.
     *
     * <p>The returned view must be closed by the acquiring thread.
     *
     * @return the acquired chunk view
     */
    public LockedChunk lockForWrite() {
        return new LockedChunk(this.unsafe, this.writeLock, true);
    }

    public void clear() {
        this.writeLock.lock();
        try {
            unsafe.clear();
            this.restoredTicks = null;
            this.loadTasks = List.of();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void close() {
        this.writeLock.lock();
        try {
            unsafe.close();
        } finally {
            this.writeLock.unlock();
        }
    }

    public void registerBlockEntity(BlockEntity blockEntity) {
        this.writeLock.lock();
        try {
            unsafe.registerBlockEntity(blockEntity);
        } finally {
            this.writeLock.unlock();
        }
    }

    public void unregisterBlockEntity(BlockEntity blockEntity) {
        this.writeLock.lock();
        try {
            unsafe.unregisterBlockEntity(blockEntity);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        this.readLock.lock();
        try {
            return unsafe.getBlockEntity(x, y, z);
        } finally {
            this.readLock.unlock();
        }
    }

    @NonNull
    @Override
    public Set<BaseBlockEntity> getBlockEntities() {
        this.readLock.lock();
        try {
            return unsafe.getBlockEntities();
        } finally {
            this.readLock.unlock();
        }
    }

    public LevelChunkPacket createChunkPacket() {
        UnsafeChunk.CLEAR_CACHE_FIELD.set(unsafe, 0);

        int dimension;
        int sectionCount;
        CloudChunkSection[] sectionSnapshot;

        this.readLock.lock();
        try {
            dimension = ((CloudLevel) unsafe.getLevel()).getDimension();
            sectionCount = unsafe.getLevel().getSectionsCount();
            sectionSnapshot = this.unsafe.getSections();
        } finally {
            this.readLock.unlock();
        }

        int highestIdx = sectionCount - 1;
        while (highestIdx >= 0 && (sectionSnapshot[highestIdx] == null || sectionSnapshot[highestIdx].isEmpty())) {
            highestIdx--;
        }

        int subChunkLimit = highestIdx + 1;

        ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
        try {
            BiomeStorage previous = null;
            for (int i = 0; i < sectionCount; i++) {
                CloudChunkSection section = sectionSnapshot[i];
                BiomeStorage bs = section != null ? section.getBiomeStorage() : defaultBiomeStorage;
                bs.writeToNetwork(buffer, previous);
                previous = bs;
            }
            buffer.writeByte(0); // border blocks (Education Edition only)

            this.writeLock.lock();
            try {
                LevelChunkPacket entry = new LevelChunkPacket();
                entry.setChunkX(this.getX());
                entry.setChunkZ(this.getZ());
                entry.setSubChunkLimit(subChunkLimit);
                entry.setRequestSubChunks(true);
                entry.setDimension(dimension);
                entry.setData(buffer.retainedDuplicate());
                return entry;
            } finally {
                this.writeLock.unlock();
            }
        } catch (Exception e) {
            log.error("Error whilst encoding chunk", e);
            throw new ChunkException("Unable to create chunk packet", e);
        } finally {
            buffer.release();
        }
    }

    public static int blockKey(Vector3i vector, int minHeight) {
        return blockKey(vector.getX(), vector.getY(), vector.getZ(), minHeight);
    }

    public static int blockKey(int x, int y, int z, int minHeight) {
        return (x & 0xf) | ((z & 0xf) << 4) | (((y - minHeight) & 0x1ff) << 8);
    }

    public static Vector3i fromBlockKey(long chunkKey, int blockKey, int minHeight) {
        int x = (blockKey & 0xf) | (fromKeyX(chunkKey) << 4);
        int z = ((blockKey >>> 4) & 0xf) | (fromKeyZ(chunkKey) << 4);
        int y = ((blockKey >>> 8) & 0x1ff) + minHeight;
        return Vector3i.from(x, y, z);
    }

    public static Vector3i fromKeyLight(long chunkKey, int blockKey, int minHeight) {
        int x = ((blockKey >>> 1) & 0xf) | (fromKeyX(chunkKey) << 4);
        int z = ((blockKey >>> 5) & 0xf) | (fromKeyZ(chunkKey) << 4);
        int y = ((blockKey >>> 9) & 0x1ff) + minHeight;
        return Vector3i.from(x, y, z);
    }

    public static int blockKeyWithLayer(int x, int y, int z, BlockLayer layer, int minHeight) {
        int encodedLayer = BlockLayerStorage.index(layer);
        return encodedLayer | ((x & 0xf) << 1) | ((z & 0xf) << 5) | (((y - minHeight) & 0x1ff) << 9);
    }

    public static long key(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    public static int fromKeyX(long key) {
        return (int) (key >> 32);
    }

    public static int fromKeyZ(long key) {
        return (int) key;
    }
}
