package org.cloudburstmc.server.diagnostics.spark;

import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Supplies server tick indices and active durations to Spark's monitoring callbacks.
 * Tick delivery uses the supplied server thread. Callback registration and tick-index reads
 * are safe from background threads.
 */
public class SparkTickMonitor implements TickHook, TickReporter {

    private final Thread owner;
    private final Set<TickHook.Callback> tickCallbacks = new CopyOnWriteArraySet<>();
    private final Set<TickReporter.Callback> durationCallbacks = new CopyOnWriteArraySet<>();
    private int nextTick;
    private volatile boolean open;
    private volatile int currentTick;

    public SparkTickMonitor(Thread owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public void start() {
        this.open = true;
    }

    /**
     * Delivers the next tick index only while monitoring is active.
     *
     * @throws IllegalStateException if called outside the server thread
     */
    public void tickStart() {
        this.requireOwner();
        if (this.open) {
            int tick = this.nextTick;
            for (TickHook.Callback callback : this.tickCallbacks) {
                callback.onTick(tick);
            }
            this.nextTick = tick + 1;
            this.currentTick = this.nextTick;
        }
    }

    /**
     * Delivers active tick duration in milliseconds only while monitoring is active.
     *
     * @throws IllegalStateException if called outside the server thread
     */
    public void tickEnd(double durationMillis) {
        this.requireOwner();
        if (this.open) {
            for (TickReporter.Callback callback : this.durationCallbacks) {
                callback.onTick(durationMillis);
            }
        }
    }

    @Override
    public int getCurrentTick() {
        return this.currentTick;
    }

    @Override
    public void addCallback(TickHook.Callback callback) {
        this.tickCallbacks.add(callback);
    }

    @Override
    public void addCallback(TickReporter.Callback callback) {
        this.durationCallbacks.add(callback);
    }

    @Override
    public void removeCallback(TickHook.Callback callback) {
        this.tickCallbacks.remove(callback);
    }

    @Override
    public void removeCallback(TickReporter.Callback callback) {
        this.durationCallbacks.remove(callback);
    }

    @Override
    public void close() {
        this.open = false;
        this.tickCallbacks.clear();
        this.durationCallbacks.clear();
    }

    private void requireOwner() {
        if (Thread.currentThread() != this.owner) {
            throw new IllegalStateException("Spark tick delivery must run on the server thread");
        }
    }
}
