package org.cloudburstmc.server;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ServerTickLoopTest {

    private static final long TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

    @Test
    void maintainsDeadlinesWithoutAddingTickWorkToThePeriod() throws InterruptedException {
        AtomicLong clock = new AtomicLong();
        List<Long> waits = new ArrayList<>();
        List<Long> starts = new ArrayList<>();
        ServerTickLoop loop = new ServerTickLoop(clock::get, nanos -> {
            waits.add(nanos);
            clock.addAndGet(nanos);
        });

        loop.run(() -> starts.size() < 4, deadline -> {
            starts.add(clock.get());
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(10));
        });

        assertEquals(List.of(0L, TICK_NANOS, 2 * TICK_NANOS, 3 * TICK_NANOS), starts);
        assertEquals(List.of(TICK_NANOS * 4 / 5, TICK_NANOS * 4 / 5, TICK_NANOS * 4 / 5), waits);
    }

    @Test
    void limitsCatchupWithoutSkippingSimulationTicks() throws InterruptedException {
        AtomicLong clock = new AtomicLong();
        List<Long> starts = new ArrayList<>();
        List<Long> deadlines = new ArrayList<>();
        ServerTickLoop loop = new ServerTickLoop(clock::get, clock::addAndGet);

        loop.run(() -> starts.size() < 7, deadline -> {
            starts.add(TimeUnit.NANOSECONDS.toMillis(clock.get()));
            deadlines.add(TimeUnit.NANOSECONDS.toMillis(deadline));
            if (starts.size() == 1) {
                clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(500));
            }
        });

        assertEquals(List.of(0L, 500L, 500L, 500L, 500L, 500L, 550L), starts);
        assertEquals(List.of(50L, 350L, 400L, 450L, 500L, 550L, 600L), deadlines);
    }

    @Test
    void retriesSpuriousWakeupsWithoutStartingEarly() throws InterruptedException {
        AtomicLong clock = new AtomicLong();
        AtomicInteger waits = new AtomicInteger();
        List<Long> starts = new ArrayList<>();
        ServerTickLoop loop = new ServerTickLoop(clock::get, nanos -> {
            waits.incrementAndGet();
            clock.addAndGet(Math.min(nanos, TimeUnit.MILLISECONDS.toNanos(10)));
        });

        loop.run(() -> starts.size() < 2, deadline -> starts.add(clock.get()));

        assertEquals(List.of(0L, TICK_NANOS), starts);
        assertEquals(5, waits.get());
    }

    @Test
    void observesShutdownWhileWaitingWithoutRunningAnotherTick() throws InterruptedException {
        AtomicLong clock = new AtomicLong();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger ticks = new AtomicInteger();
        ServerTickLoop loop = new ServerTickLoop(clock::get, nanos -> running.set(false));

        loop.run(running::get, deadline -> ticks.incrementAndGet());

        assertEquals(1, ticks.get());
    }

    @Test
    void propagatesTickFailureInsteadOfRetryingIt() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger attempts = new AtomicInteger();
        ServerTickLoop loop = new ServerTickLoop(clock::get, clock::addAndGet);
        IllegalStateException failure = new IllegalStateException("failed tick");

        assertSame(failure, assertThrows(IllegalStateException.class, () -> loop.run(() -> true, deadline -> {
            attempts.incrementAndGet();
            throw failure;
        })));
        assertEquals(1, attempts.get());
        assertThrows(IllegalStateException.class, () -> loop.run(() -> false, deadline -> {}));
    }

    @Test
    void exitsWhenInterruptedWhileWaiting() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger ticks = new AtomicInteger();
        ServerTickLoop loop = new ServerTickLoop(clock::get, nanos -> Thread.currentThread().interrupt());

        assertThrows(InterruptedException.class, () -> loop.run(() -> true, deadline -> ticks.incrementAndGet()));
        assertEquals(1, ticks.get());
    }

    @Test
    void handlesNanoTimeWraparoundAndIndependentHeartbeat() throws InterruptedException {
        long initial = Long.MAX_VALUE - TICK_NANOS / 2;
        AtomicLong clock = new AtomicLong(initial);
        List<Long> starts = new ArrayList<>();
        ServerTickLoop loop = new ServerTickLoop(clock::get, clock::addAndGet);

        assertEquals(0, loop.unresponsiveNanos());
        loop.run(() -> starts.size() < 2, deadline -> starts.add(clock.get()));

        assertEquals(List.of(initial, initial + TICK_NANOS), starts);
        assertEquals(0, loop.unresponsiveNanos());

        clock.addAndGet(TimeUnit.SECONDS.toNanos(3));
        assertEquals(TimeUnit.SECONDS.toNanos(3), loop.unresponsiveNanos());
    }
}
