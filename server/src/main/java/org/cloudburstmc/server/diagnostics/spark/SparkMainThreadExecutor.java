package org.cloudburstmc.server.diagnostics.spark;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Runs server-thread work immediately and queues cross-thread work until the next drain.
 */
public class SparkMainThreadExecutor implements Executor, AutoCloseable {

    private final Thread owner;
    private final Object lock = new Object();
    private final ConcurrentLinkedQueue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private boolean closed;

    public SparkMainThreadExecutor(Thread owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /**
     * Runs work immediately on the owner thread or queues it for that thread to drain.
     *
     * @throws RejectedExecutionException if the executor is closed
     */
    @Override
    public void execute(@NonNull Runnable task) {
        Objects.requireNonNull(task, "task");
        synchronized (this.lock) {
            if (this.closed) {
                throw new RejectedExecutionException("Spark is stopping");
            }
            if (Thread.currentThread() != this.owner) {
                this.pending.add(task);
                return;
            }
        }

        task.run();
    }

    /**
     * Runs queued tasks on the owner thread until the queue is empty or a task throws.
     *
     * @throws IllegalStateException if called from another thread
     */
    public void drain() {
        this.requireOwner();
        Runnable task;
        while ((task = this.pending.poll()) != null) {
            task.run();
        }
    }

    /**
     * Stops accepting work and drains pending tasks on the server thread.
     *
     * @throws IllegalStateException if called from another thread
     */
    @Override
    public void close() {
        this.requireOwner();
        synchronized (this.lock) {
            this.closed = true;
        }
        this.drain();
    }

    /**
     * Rejects new work and discards pending tasks during emergency shutdown without running them off-thread.
     */
    public void shutdownNow() {
        synchronized (this.lock) {
            this.closed = true;
            this.pending.clear();
        }
    }

    private void requireOwner() {
        if (Thread.currentThread() != this.owner) {
            throw new IllegalStateException("Spark synchronous tasks must be drained on the server thread");
        }
    }
}
