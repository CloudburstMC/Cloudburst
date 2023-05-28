package org.cloudburstmc.api.level.chunk;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public abstract class LockableChunk implements Chunk, Lock {
    private final Lock lock;

    public LockableChunk(Lock lock) {
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
    public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
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

}
