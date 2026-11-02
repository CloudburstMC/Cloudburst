package org.cloudburstmc.server;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Runs the server at 20 ticks per second using monotonic deadlines.
 * At most five overdue ticks are retained after a stall. Simulation ticks are never skipped.
 */
public class ServerTickLoop {

    private static final long TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);
    private static final int MAX_CATCHUP_TICKS = 5;

    private final LongSupplier clock;
    private final LongConsumer waitStrategy;
    private volatile Thread owner;
    private volatile boolean started;
    private volatile long heartbeatNanos;

    public ServerTickLoop() {
        this(System::nanoTime, nanos -> LockSupport.parkNanos("waiting for server tick", nanos));
    }

    /**
     * Creates a loop with a monotonic nanosecond clock and an interruptible wait operation.
     * The wait may return early and is retried until the deadline or shutdown.
     */
    public ServerTickLoop(LongSupplier clock, LongConsumer waitStrategy) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.waitStrategy = Objects.requireNonNull(waitStrategy, "waitStrategy");
    }

    /**
     * Runs ticks on the calling thread until stopped or interrupted.
     * Tick failures propagate to the caller rather than retrying a failed tick indefinitely.
     *
     * @param running whether the loop should continue
     * @param tick    tick body receiving the next tick's monotonic nanosecond deadline
     * @throws IllegalStateException if this loop has already been run
     * @throws InterruptedException  if the owner thread is interrupted
     */
    public void run(BooleanSupplier running, LongConsumer tick) throws InterruptedException {
        Objects.requireNonNull(running, "running");
        Objects.requireNonNull(tick, "tick");
        synchronized (this) {
            if (this.owner != null) {
                throw new IllegalStateException("The server tick loop cannot be restarted");
            }
            this.owner = Thread.currentThread();
        }

        long deadline = this.clock.getAsLong();
        this.heartbeatNanos = deadline;
        this.started = true;

        while (running.getAsBoolean()) {
            long remaining;
            while ((remaining = deadline - this.clock.getAsLong()) > 0 && running.getAsBoolean()) {
                checkInterrupted();
                this.waitStrategy.accept(remaining);
            }
            checkInterrupted();
            if (!running.getAsBoolean()) {
                break;
            }

            long now = this.clock.getAsLong();
            long overdue = (now - deadline) / TICK_NANOS;
            if (overdue >= MAX_CATCHUP_TICKS) {
                deadline += (overdue - MAX_CATCHUP_TICKS + 1) * TICK_NANOS;
            }

            long nextDeadline = deadline + TICK_NANOS;
            this.heartbeatNanos = now;
            tick.accept(nextDeadline);
            this.heartbeatNanos = this.clock.getAsLong();
            deadline = nextDeadline;
        }
    }

    /**
     * Wakes the owner so it can observe a shutdown request without waiting for its deadline.
     */
    public void wakeUp() {
        Thread thread = this.owner;
        if (thread != null) {
            LockSupport.unpark(thread);
        }
    }

    /**
     * Returns time since the latest tick heartbeat, or zero before observation begins.
     */
    public long unresponsiveNanos() {
        if (!this.started) {
            return 0;
        }

        long heartbeat = this.heartbeatNanos;
        return this.clock.getAsLong() - heartbeat;
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("Server tick loop interrupted");
        }
    }
}
