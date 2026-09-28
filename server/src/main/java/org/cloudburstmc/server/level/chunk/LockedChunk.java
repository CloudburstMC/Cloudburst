package org.cloudburstmc.server.level.chunk;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Direct chunk view held under a read or write lock.
 *
 * <p>The view must be closed by the thread that acquired it and must not be
 * used after it is closed.
 */
public final class LockedChunk implements Chunk, AutoCloseable {
    private final UnsafeChunk unsafe;
    private final Lock lock;
    private final boolean writable;
    private boolean closed;

    LockedChunk(UnsafeChunk unsafe, Lock lock, boolean writable) {
        this.unsafe = unsafe;
        this.lock = lock;
        this.writable = writable;
        this.lock.lock();
    }

    @Override
    public int getX() {
        return this.unsafe().getX();
    }

    @Override
    public int getZ() {
        return this.unsafe().getZ();
    }

    @NonNull
    @Override
    public Level getLevel() {
        return this.unsafe().getLevel();
    }

    @Override
    public boolean isGenerated() {
        return this.unsafe().isGenerated();
    }

    @NonNull
    @Override
    public CloudChunkSection getOrCreateSection(int y) {
        return this.writableUnsafe().getOrCreateSection(y);
    }

    @Nullable
    @Override
    public CloudChunkSection getSection(int y) {
        return this.unsafe().getSection(y);
    }

    @NonNull
    @Override
    public CloudChunkSection[] getSections() {
        return this.unsafe().getSections();
    }

    @NonNull
    @Override
    public BlockState getBlockState(int x, int y, int z, BlockLayer layer) {
        return this.unsafe().getBlockState(x, y, z, layer);
    }

    @Override
    public BlockState setBlockState(int x, int y, int z, BlockLayer layer, BlockState blockState) {
        return this.writableUnsafe().setBlockState(x, y, z, layer, blockState);
    }

    @Override
    public int getBiome(int x, int y, int z) {
        return this.unsafe().getBiome(x, y, z);
    }

    @Override
    public void setBiome(int x, int y, int z, int biome) {
        this.writableUnsafe().setBiome(x, y, z, biome);
    }

    @Override
    public void fillColumnBiome(int x, int z, int biomeId) {
        this.writableUnsafe().fillColumnBiome(x, z, biomeId);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return this.unsafe().getSkyLight(x, y, z);
    }

    @Override
    public void setSkyLight(int x, int y, int z, int level) {
        this.writableUnsafe().setSkyLight(x, y, z, level);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return this.unsafe().getBlockLight(x, y, z);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int level) {
        this.writableUnsafe().setBlockLight(x, y, z, level);
    }

    @Override
    public int getHighestBlock(int x, int z) {
        return this.unsafe().getHighestBlock(x, z);
    }

    @Override
    public int @NonNull [] getHeightMap() {
        return this.unsafe().getHeightMap();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.unsafe().getBlockEntity(x, y, z);
    }

    @NonNull
    @Override
    public Set<CloudPlayer> getPlayers() {
        return this.unsafe().getPlayers();
    }

    @NonNull
    @Override
    public Set<CloudEntity> getEntities() {
        return this.unsafe().getEntities();
    }

    @NonNull
    @Override
    public Set<BaseBlockEntity> getBlockEntities() {
        return this.unsafe().getBlockEntities();
    }

    @Override
    public Set<? extends Player> getViewers() {
        return this.unsafe().getViewers();
    }

    @Override
    public void close() {
        Preconditions.checkState(!this.closed, "chunk lock is already closed");
        this.closed = true;
        this.lock.unlock();
    }

    private UnsafeChunk unsafe() {
        Preconditions.checkState(!this.closed, "chunk lock is closed");
        return this.unsafe;
    }

    private UnsafeChunk writableUnsafe() {
        UnsafeChunk unsafe = this.unsafe();
        Preconditions.checkState(this.writable, "chunk view is read-only");
        return unsafe;
    }
}
