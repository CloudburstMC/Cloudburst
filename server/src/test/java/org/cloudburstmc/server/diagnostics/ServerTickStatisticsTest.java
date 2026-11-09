package org.cloudburstmc.server.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerTickStatisticsTest {

    @Test
    void excludesStartupAndPacingFromActiveTickDuration() {
        AtomicLong clock = new AtomicLong(TimeUnit.HOURS.toNanos(1));
        ServerTickStatistics statistics = new ServerTickStatistics(clock::get);
        statistics.start();

        for (int tick = 0; tick < 20; tick++) {
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(50));
            statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(2));
        }

        ServerTickStatistics.Snapshot snapshot = statistics.snapshot();
        assertEquals(20, snapshot.oneMinuteTps());
        assertEquals(20, snapshot.fiveMinuteTps());
        assertEquals(20, snapshot.fifteenMinuteTps());
        assertEquals(new ServerTickStatistics.Durations(2, 2, 2), snapshot.fiveSecondMspt());
    }

    @Test
    void measuresTpsOverIndependentElapsedTimeWindows() {
        ServerTickStatistics statistics = statisticsWithRecentSlowdown();
        ServerTickStatistics.Snapshot snapshot = statistics.snapshot();

        assertEquals(10, snapshot.oneMinuteTps());
        assertEquals(18, snapshot.fiveMinuteTps());
        assertEquals(19.333333333, snapshot.fifteenMinuteTps(), 0.000000001);
        assertEquals(new ServerTickStatistics.Durations(100, 100, 100), snapshot.oneMinuteMspt());
    }

    @Test
    void expiresDurationsByTimeRatherThanNumberOfTicks() {
        AtomicLong clock = new AtomicLong();
        ServerTickStatistics statistics = new ServerTickStatistics(clock::get);
        statistics.start();

        clock.set(TimeUnit.SECONDS.toNanos(1));
        statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(1));
        clock.set(TimeUnit.SECONDS.toNanos(6));
        statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(40));
        clock.set(TimeUnit.SECONDS.toNanos(11));
        statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(50));
        clock.set(TimeUnit.SECONDS.toNanos(61));
        statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(100));

        ServerTickStatistics.Snapshot snapshot = statistics.snapshot();
        assertEquals(new ServerTickStatistics.Durations(100, 100, 100), snapshot.fiveSecondMspt());
        assertEquals(snapshot.fiveSecondMspt(), snapshot.tenSecondMspt());
        assertEquals(190d / 3, snapshot.oneMinuteMspt().average());
        assertEquals(40, snapshot.oneMinuteMspt().minimum());
        assertEquals(100, snapshot.oneMinuteMspt().maximum());

        clock.set(TimeUnit.SECONDS.toNanos(961));
        snapshot = statistics.snapshot();
        assertEquals(0, snapshot.fifteenMinuteTps());
        assertEquals(new ServerTickStatistics.Durations(0, 0, 0), snapshot.oneMinuteMspt());
    }

    @Test
    void requiresStartedObservationValidDurationsAndMonotonicTime() {
        AtomicLong clock = new AtomicLong(-100);
        ServerTickStatistics statistics = new ServerTickStatistics(clock::get);

        assertEquals(20, statistics.snapshot().oneMinuteTps());
        assertThrows(IllegalStateException.class, () -> statistics.recordTick(1));

        statistics.start();
        assertThrows(IllegalStateException.class, statistics::start);
        assertThrows(IllegalArgumentException.class, () -> statistics.recordTick(-1));

        clock.set(-50);
        statistics.recordTick(10);
        assertEquals(20, statistics.snapshot().oneMinuteTps());

        clock.set(-75);
        assertThrows(IllegalStateException.class, statistics::snapshot);
    }

    private static ServerTickStatistics statisticsWithRecentSlowdown() {
        AtomicLong clock = new AtomicLong();
        ServerTickStatistics statistics = new ServerTickStatistics(clock::get);
        statistics.start();

        for (int tick = 0; tick < 18_000; tick++) {
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(50));
            statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(1));
        }

        for (int tick = 0; tick < 600; tick++) {
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(100));
            statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(100));
        }

        return statistics;
    }
}
