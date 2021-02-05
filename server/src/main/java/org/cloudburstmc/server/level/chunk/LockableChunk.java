package org.cloudburstmc.server.level.chunk;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@NotThreadSafe
public final class LockableChunk implements Chunk, Lock {
    private final UnsafeChunk unsafe;
    private final Lock lock;

    LockableChunk(UnsafeChunk unsafe, Lock lock) {
        this.unsafe = unsafe;
        this.lock = lock;
    }

    @Override
    public void lock() {
        this.lock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return this.lock.tryLock();
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        return this.lock.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        this.lock.unlock();
    }

    @Override
    public Condition newCondition() {
        return lock.newCondition();
    }

    @Nonnull
    @Override
    public CloudChunkSection getOrCreateSection(int y) {
        return unsafe.getOrCreateSection(y);
    }

    @Nullable
    @Override
    public CloudChunkSection getSection(int y) {
        return unsafe.getSection(y);
    }

    @Nonnull
    @Override
    public CloudChunkSection[] getSections() {
        CloudChunkSection[] sections = unsafe.getSections();
        return Arrays.copyOf(sections, sections.length);
    }

    @Nonnull
    @Override
    public BlockState getBlock(int x, int y, int z, int layer) {
        return unsafe.getBlock(x, y, z, layer);
    }

    @Override
    public BlockState getAndSetBlock(int x, int y, int z, int layer, BlockState blockState) {
        return unsafe.getAndSetBlock(x, y, z, layer, blockState);
    }

    @Override
    public void setBlock(int x, int y, int z, int layer, BlockState blockState) {
        this.unsafe.setBlock(x, y, z, layer, blockState);
    }

    @Override
    public int getBiome(int x, int z) {
        return this.unsafe.getBiome(x, z);
    }

    @Override
    public void setBiome(int x, int z, int biome) {
        this.unsafe.setBiome(x, z, biome);
    }

    @Override
    public byte getSkyLight(int x, int y, int z) {
        return this.unsafe.getSkyLight(x, y, z);
    }

    @Override
    public void setSkyLight(int x, int y, int z, int level) {
        this.unsafe.setSkyLight(x, y, z, level);
    }

    @Override
    public byte getBlockLight(int x, int y, int z) {
        return this.unsafe.getBlockLight(x, y, z);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int level) {
        this.unsafe.setBlockLight(x, y, z, level);
    }

    @Override
    public int getHighestBlock(int x, int z) {
        return this.unsafe.getHighestBlock(x, z);
    }

    @Override
    public void addEntity(@Nonnull Entity entity) {
        this.unsafe.addEntity(entity);
    }

    @Override
    public void removeEntity(Entity entity) {
        this.unsafe.removeEntity(entity);
    }

    @Override
    public void addBlockEntity(BlockEntity blockEntity) {
        this.unsafe.addBlockEntity(blockEntity);
    }

    @Override
    public void removeBlockEntity(BlockEntity blockEntity) {
        this.unsafe.removeBlockEntity(blockEntity);
    }

    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        return this.unsafe.getBlockEntity(x, y, z);
    }

    @Override
    public int getX() {
        return this.unsafe.getX();
    }

    @Override
    public int getZ() {
        return this.unsafe.getZ();
    }

    @Nonnull
    @Override
    public CloudLevel getLevel() {
        return this.unsafe.getLevel();
    }

    @Nonnull
    @Override
    public byte[] getBiomeArray() {
        return this.unsafe.getBiomeArray().clone();
    }

    @Nonnull
    @Override
    public int[] getHeightMapArray() {
        return this.unsafe.getHeightMapArray().clone();
    }

    @Nonnull
    @Override
    public Set<CloudPlayer> getPlayers() {
        return this.unsafe.getPlayers();
    }

    @Nonnull
    @Override
    public Set<Entity> getEntities() {
        return this.unsafe.getEntities();
    }

    @Nonnull
    @Override
    public Collection<BlockEntity> getBlockEntities() {
        return this.unsafe.getBlockEntities();
    }

    @Override
    public int getState() {
        return this.unsafe.getState();
    }

    @Override
    public int setState(int next) {
        return this.unsafe.setState(next);
    }

    @Override
    public boolean isDirty() {
        return this.unsafe.isDirty();
    }

    @Override
    public void setDirty(boolean dirty) {
        this.unsafe.setDirty(dirty);
    }

    @Override
    public boolean clearDirty() {
        return this.unsafe.clearDirty();
    }

    @Override
    public void clear() {
        this.unsafe.clear();
    }
}
