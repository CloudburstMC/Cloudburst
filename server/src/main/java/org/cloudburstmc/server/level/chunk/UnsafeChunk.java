package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * Mutable chunk storage guarded by {@link CloudChunk}.
 *
 * <p>Direct access is limited to construction and lifecycle code or code that
 * holds the owning chunk's corresponding read or write lock.
 */
final class UnsafeChunk implements Chunk, Closeable {
    private static final int INITIALIZATION_NOT_STARTED = 0;
    private static final int INITIALIZATION_IN_PROGRESS = 1;
    private static final int INITIALIZATION_COMPLETE = 2;

    static final AtomicIntegerFieldUpdater<UnsafeChunk> CLEAR_CACHE_FIELD = AtomicIntegerFieldUpdater
            .newUpdater(UnsafeChunk.class, "clearCache");
    private static final AtomicLongFieldUpdater<UnsafeChunk> CONTENT_REVISION_FIELD = AtomicLongFieldUpdater
            .newUpdater(UnsafeChunk.class, "contentRevision");
    private static final AtomicLongFieldUpdater<UnsafeChunk> SAVED_REVISION_FIELD = AtomicLongFieldUpdater
            .newUpdater(UnsafeChunk.class, "savedRevision");
    private static final AtomicIntegerFieldUpdater<UnsafeChunk> INITIALIZED_FIELD = AtomicIntegerFieldUpdater
            .newUpdater(UnsafeChunk.class, "initialized");
    private static final AtomicReferenceFieldUpdater<UnsafeChunk, ChunkGenerationStatus> STATUS_FIELD =
            AtomicReferenceFieldUpdater.newUpdater(UnsafeChunk.class, ChunkGenerationStatus.class, "generationStatus");
    private static final AtomicIntegerFieldUpdater<UnsafeChunk> CLOSED_FIELD = AtomicIntegerFieldUpdater
            .newUpdater(UnsafeChunk.class, "closed");
    private final int x;

    private final int z;

    private final Level level;

    private final CloudChunkSection[] sections;

    private final Set<CloudPlayer> players = Collections.newSetFromMap(new IdentityHashMap<>());

    private final Set<CloudEntity> entities = Collections.newSetFromMap(new IdentityHashMap<>());

    private final Int2ObjectMap<BaseBlockEntity> tiles = new Int2ObjectOpenHashMap<>();

    private final int[] heightMap;

    private volatile long contentRevision;

    private volatile long savedRevision;

    private volatile int initialized;

    private volatile ChunkGenerationStatus generationStatus = ChunkGenerationStatus.NEW;

    private volatile int closed;

    private volatile int clearCache;

    UnsafeChunk(int x, int z, Level level) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.sections = new CloudChunkSection[level.getSectionsCount()];
        this.heightMap = new int[CloudChunk.ARRAY_SIZE];
        this.initialized = INITIALIZATION_COMPLETE;
    }

    UnsafeChunk(int x, int z, Level level, CloudChunkSection[] sections, int[] heightMap,
                ChunkGenerationStatus generationStatus) {
        this.x = x;
        this.z = z;
        this.level = level;
        Preconditions.checkNotNull(sections, "sections");
        this.sections = Arrays.copyOf(sections, level.getSectionsCount());
        Preconditions.checkNotNull(heightMap, "heightMap");
        this.heightMap = Arrays.copyOf(heightMap, CloudChunk.ARRAY_SIZE);
        this.generationStatus = Preconditions.checkNotNull(generationStatus, "generationStatus");
    }

    static void checkBounds(int x, int y, int z) {
        checkBounds(x, z);
    }

    static void checkBounds(int x, int z) {
        checkElementIndex(x, 16, "x coordinate");
        checkElementIndex(z, 16, "z coordinate");
    }

    static int get2dIndex(int x, int z) {
        return z << 4 | x;
    }

    public boolean beginInitialization() {
        return INITIALIZED_FIELD.compareAndSet(this, INITIALIZATION_NOT_STARTED, INITIALIZATION_IN_PROGRESS);
    }

    public void completeInitialization() {
        Preconditions.checkState(
                INITIALIZED_FIELD.compareAndSet(this, INITIALIZATION_IN_PROGRESS, INITIALIZATION_COMPLETE),
                "chunk initialization is not in progress"
        );
    }

    @NonNull
    @Override
    public CloudChunkSection getOrCreateSection(int y) {
        checkElementIndex(y, sections.length, "section Y");

        CloudChunkSection section = this.sections[y];
        if (section == null) {
            section = new CloudChunkSection(this.level.getServer().getBlockRegistry());
            this.sections[y] = section;
            this.markDirty();
        }
        return section;
    }

    @Nullable
    @Override
    public CloudChunkSection getSection(int y) {
        checkElementIndex(y, sections.length, "section Y");
        return this.sections[y];
    }
    @NonNull
    @Override
    public CloudChunkSection[] getSections() {
        return Arrays.copyOf(this.sections, this.sections.length);
    }


    @NonNull
    @Override
    public BlockState getBlockState(int x, int y, int z, BlockLayer layer) {
        checkBounds(x, y, z);
        if (this.level.isOutsideBuildHeight(y)) {
            return BlockStates.AIR;
        }
        CloudChunkSection section = this.getSection(this.level.getSectionIndex(y));
        BlockState blockState;
        if (section == null) {
            blockState = BlockStates.AIR;
        } else {
            blockState = section.getBlockState(x, y & 0xf, z, layer);
        }
        return blockState;
    }

    @NonNull
    @Override
    public BlockState setBlockState(int x, int y, int z, BlockLayer layer, BlockState blockState) {
        checkBounds(x, y, z);
        if (this.level.isOutsideBuildHeight(y)) {
            return BlockStates.AIR;
        }

        CloudChunkSection section = this.getSection(this.level.getSectionIndex(y));
        if (section == null) {
            if (blockState.getType() == BlockTypes.AIR) {
                // Setting air in an empty section.
                return BlockStates.AIR;
            }
            section = this.getOrCreateSection(this.level.getSectionIndex(y));
        }

        BlockState previousBlockState = section.setBlockState(x, y & 0xf, z, layer, blockState);
        this.markDirty();
        return previousBlockState;
    }

    @Override
    public int getBiome(int x, int y, int z) {
        checkBounds(x, z);
        if (this.level.isOutsideBuildHeight(y)) {
            return CloudChunkSection.DEFAULT_BIOME_ID;
        }

        CloudChunkSection section = this.getSection(this.level.getSectionIndex(y));
        if (section == null) {
            return CloudChunkSection.DEFAULT_BIOME_ID;
        }

        return section.getBiome(x, y & 0xf, z);
    }

    @Override
    public void setBiome(int x, int y, int z, int biome) {
        checkBounds(x, z);
        if (this.level.isOutsideBuildHeight(y)) {
            return;
        }
        this.getOrCreateSection(this.level.getSectionIndex(y)).setBiome(x, y & 0xf, z, biome);
        this.markDirty();
    }

    @Override
    public void fillColumnBiome(int x, int z, int biomeId) {
        checkBounds(x, z);
        int sectionCount = this.level.getSectionsCount();
        for (int i = 0; i < sectionCount; i++) {
            this.getOrCreateSection(i).fillColumnBiome(x, z, biomeId);
        }
        this.markDirty();
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        checkBounds(x, y, z);
        CloudChunkSection section = this.getSection(this.level.getSectionIndex(y));
        return section == null ? 0 : section.getSkyLight(x, y & 0xf, z);
    }

    @Override
    public void setSkyLight(int x, int y, int z, int level) {
        checkBounds(x, y, z);
        this.getOrCreateSection(this.level.getSectionIndex(y)).setSkyLight(x, y & 0xf, z, level);
        this.markDirty();
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        checkBounds(x, y, z);
        CloudChunkSection section = this.getSection(this.level.getSectionIndex(y));
        return section == null ? 0 : section.getBlockLight(x, y & 0xf, z);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int level) {
        checkBounds(x, y, z);
        this.getOrCreateSection(this.level.getSectionIndex(y)).setBlockLight(x, y & 0xf, z, level);
        this.markDirty();
    }

    @Override
    public int getHighestBlock(int x, int z) {
        checkBounds(x, z);
        for (int sectionIdx = this.sections.length - 1; sectionIdx >= 0; sectionIdx--) {
            CloudChunkSection section = this.sections[sectionIdx];
            if (section != null) {
                for (int y = 15; y >= 0; y--) {
                    if (section.getBlockState(x, y, z, BlockLayer.PRIMARY) != BlockStates.AIR) {
                        return ((sectionIdx + this.level.getMinSectionY()) << 4) | y;
                    }
                }
            }
        }
        return -1;
    }

    public void registerEntity(@NonNull Entity entity) {
        Preconditions.checkNotNull(entity, "entity");
        if (entity instanceof CloudPlayer) {
            this.players.add((CloudPlayer) entity);
        } else if (this.entities.add((CloudEntity) entity) && this.isInitializationComplete()) {
            this.markDirty();
        }
    }

    public void unregisterEntity(Entity entity) {
        Preconditions.checkNotNull(entity, "entity");
        if (entity instanceof CloudPlayer) {
            this.players.remove(entity);
        } else if (this.entities.remove(entity) && this.isInitializationComplete()) {
            this.markDirty();
        }
    }

    public void registerBlockEntity(BlockEntity blockEntity) {
        Preconditions.checkNotNull(blockEntity, "blockEntity");
        int hash = CloudChunk.blockKey(blockEntity.getPosition(), this.level.getMinHeight());
        if (this.tiles.put(hash, (BaseBlockEntity) blockEntity) != blockEntity && this.isInitializationComplete()) {
            this.markDirty();
        }
    }

    public void unregisterBlockEntity(BlockEntity blockEntity) {
        Preconditions.checkNotNull(blockEntity, "blockEntity");
        int hash = CloudChunk.blockKey(blockEntity.getPosition(), this.level.getMinHeight());
        if (this.tiles.remove(hash) == blockEntity && this.isInitializationComplete()) {
            this.markDirty();
        }
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        checkBounds(x, y, z);
        return this.tiles.get(CloudChunk.blockKey(x, y, z, this.level.getMinHeight()));
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }
    @NonNull
    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public int @NonNull [] getHeightMap() {
        return this.heightMap.clone();
    }


    @NonNull
    @Override
    public Set<CloudPlayer> getPlayers() {
        return Set.copyOf(this.players);
    }

    @NonNull
    @Override
    public Set<CloudEntity> getEntities() {
        return Set.copyOf(this.entities);
    }

    @NonNull
    @Override
    public Set<BaseBlockEntity> getBlockEntities() {
        return Set.copyOf(this.tiles.values());
    }

    public ChunkGenerationStatus getGenerationStatus() {
        return this.generationStatus;
    }

    public void advanceGenerationStatus(ChunkGenerationStatus nextStatus) {
        Preconditions.checkNotNull(nextStatus, "nextStatus");
        STATUS_FIELD.accumulateAndGet(this, nextStatus, (current, next) -> {
            Preconditions.checkState(current.compareTo(next) < 0,
                    "invalid generation status transition: %s => %s", current, next);
            return next;
        });
        this.markDirty();
    }

    @Override
    public boolean isGenerated() {
        return this.generationStatus.isAtLeast(ChunkGenerationStatus.GENERATED);
    }

    public boolean isPopulated() {
        return this.generationStatus.isAtLeast(ChunkGenerationStatus.POPULATED);
    }

    public boolean isFinished() {
        return this.generationStatus == ChunkGenerationStatus.FINISHED;
    }

    /**
     * Whether the chunk has changed since it was last loaded or saved.
     *
     * @return dirty
     */
    public boolean isDirty() {
        return this.isGenerated() && this.contentRevision != this.savedRevision;
    }

    public long captureSaveRevision() {
        return this.contentRevision;
    }

    public void markDirty() {
        CLEAR_CACHE_FIELD.set(this, 1);
        CONTENT_REVISION_FIELD.incrementAndGet(this);
    }

    private boolean isInitializationComplete() {
        return this.initialized == INITIALIZATION_COMPLETE;
    }

    public void acknowledgeSave(long saveRevision) {
        Preconditions.checkArgument(saveRevision <= this.contentRevision,
                "saved revision %s is newer than current revision %s", saveRevision, this.contentRevision);
        SAVED_REVISION_FIELD.accumulateAndGet(this, saveRevision, Math::max);
    }

    /**
     * Clear chunk to a state as if it was not generated.
     */
    public void clear() {
        Arrays.fill(this.sections, null);
        Arrays.fill(this.heightMap, 0);
        this.tiles.clear();
        this.entities.clear();
        this.generationStatus = ChunkGenerationStatus.NEW;
        this.markDirty();
    }

    @Override
    public void close() {
        if (CLOSED_FIELD.compareAndSet(this, 0, 1)) {
            Entity[] entitySnapshot = this.entities.toArray(new Entity[0]);
            for (Entity entity : entitySnapshot) {
                if (entity instanceof Player) {
                    continue;
                }
                entity.close();
            }

            BaseBlockEntity[] tileSnapshot = this.tiles.values().toArray(new BaseBlockEntity[0]);
            for (BaseBlockEntity tile : tileSnapshot) {
                tile.close();
            }
            clear();
        }
    }

    @Override
    public Set<CloudPlayer> getViewers() {
        return ((CloudLevel) this.level).getChunkPlayers(this.x, this.z);
    }
}
