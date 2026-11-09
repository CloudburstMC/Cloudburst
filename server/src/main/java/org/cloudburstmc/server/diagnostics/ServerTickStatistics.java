package org.cloudburstmc.server.diagnostics;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Tracks completed ticks and active tick durations over elapsed-time windows.
 * Recording and snapshots are thread-safe.
 *
 * <p>TPS is capped at 20 and uses the observed uptime until a complete window is available.
 * Duration windows include ticks that completed during the window.</p>
 */
public class ServerTickStatistics {

    private static final long SECOND = TimeUnit.SECONDS.toNanos(1);
    private static final long HISTORY = 900 * SECOND;

    private final LongSupplier clock;
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private boolean started;
    private long startedAt;
    private long lastTime;

    public ServerTickStatistics() {
        this(System::nanoTime);
    }

    /**
     * Creates statistics using a monotonic nanosecond clock.
     *
     * @param clock elapsed-time source, such as {@link System#nanoTime()}
     */
    public ServerTickStatistics(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Starts observation when the server enters its tick loop, excluding startup time.
     *
     * @throws IllegalStateException if observation has already started
     */
    public synchronized void start() {
        if (this.started) {
            throw new IllegalStateException("Tick statistics have already started");
        }

        this.startedAt = this.clock.getAsLong();
        this.lastTime = this.startedAt;
        this.started = true;
    }

    /**
     * Records a tick at its completion time.
     *
     * @param durationNanos active tick duration, excluding time spent waiting for the next tick
     * @throws IllegalArgumentException if the duration is negative
     * @throws IllegalStateException    if observation has not started or the clock moves backwards
     */
    public synchronized void recordTick(long durationNanos) {
        if (!this.started) {
            throw new IllegalStateException("Tick statistics have not started");
        }

        if (durationNanos < 0) {
            throw new IllegalArgumentException("Tick duration must not be negative");
        }

        long now = this.time();
        this.prune(now);
        this.samples.addLast(new Sample(now, durationNanos));
    }

    /**
     * Returns TPS and tick durations from a single observation time.
     * Before observation starts, TPS is 20 and durations are zero.
     *
     * @return an immutable snapshot with zero durations for windows without completed ticks
     * @throws IllegalStateException if the clock moves backwards
     */
    public synchronized Snapshot snapshot() {
        if (!this.started) {
            return new Snapshot(20, 20, 20, Durations.EMPTY, Durations.EMPTY, Durations.EMPTY);
        }

        long now = this.time();
        this.prune(now);
        long oneMinuteCount = 0;
        long fiveMinuteCount = 0;
        DurationAccumulator fiveSeconds = new DurationAccumulator();
        DurationAccumulator tenSeconds = new DurationAccumulator();
        DurationAccumulator oneMinute = new DurationAccumulator();

        for (Sample sample : this.samples) {
            long age = now - sample.completedAt();
            if (age < 300 * SECOND) {
                fiveMinuteCount++;
            }

            if (age < 60 * SECOND) {
                oneMinuteCount++;
                oneMinute.add(sample.durationNanos());
            }

            if (age < 10 * SECOND) {
                tenSeconds.add(sample.durationNanos());
            }

            if (age < 5 * SECOND) {
                fiveSeconds.add(sample.durationNanos());
            }
        }

        long uptime = now - this.startedAt;
        return new Snapshot(
                tps(oneMinuteCount, uptime, 60 * SECOND),
                tps(fiveMinuteCount, uptime, 300 * SECOND),
                tps(this.samples.size(), uptime, HISTORY),
                fiveSeconds.snapshot(), tenSeconds.snapshot(), oneMinute.snapshot()
        );
    }

    private long time() {
        long now = this.clock.getAsLong();
        if (now - this.lastTime < 0) {
            throw new IllegalStateException("Tick statistics require a monotonic clock");
        }

        this.lastTime = now;
        return now;
    }

    private void prune(long now) {
        while (!this.samples.isEmpty() && now - this.samples.getFirst().completedAt() >= HISTORY) {
            this.samples.removeFirst();
        }
    }

    private static double tps(long count, long uptime, long window) {
        long elapsed = Math.min(uptime, window);
        return elapsed == 0 ? 20 : Math.min(20, count * (double) SECOND / elapsed);
    }

    /**
     * Tick rates in ticks per second and active duration summaries in milliseconds.
     */
    public record Snapshot(double oneMinuteTps, double fiveMinuteTps, double fifteenMinuteTps, Durations fiveSecondMspt, Durations tenSecondMspt, Durations oneMinuteMspt) {
    }

    /**
     * Average, minimum and maximum active tick durations in milliseconds.
     */
    public record Durations(double average, double minimum, double maximum) {
        private static final Durations EMPTY = new Durations(0, 0, 0);
    }

    private record Sample(long completedAt, long durationNanos) {
    }

    private static class DurationAccumulator {
        private long count;
        private double total;
        private long minimum = Long.MAX_VALUE;
        private long maximum;

        private void add(long duration) {
            this.count++;
            this.total += duration;
            this.minimum = Math.min(this.minimum, duration);
            this.maximum = Math.max(this.maximum, duration);
        }

        private Durations snapshot() {
            return this.count == 0 ? Durations.EMPTY : new Durations(
                    this.total / this.count / 1_000_000d,
                    this.minimum / 1_000_000d,
                    this.maximum / 1_000_000d
            );
        }
    }
}
