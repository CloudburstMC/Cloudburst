package org.cloudburstmc.server.diagnostics.spark;

import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class SparkTickMonitorTest {

    @Test
    void reportsTickIndicesAndUnmodifiedMillisecondDurations() {
        SparkTickMonitor monitor = new SparkTickMonitor(Thread.currentThread());
        List<Integer> ticks = new ArrayList<>();
        List<Double> durations = new ArrayList<>();
        monitor.addCallback((TickHook.Callback) ticks::add);
        monitor.addCallback((TickReporter.Callback) durations::add);

        monitor.start();
        monitor.tickStart();
        monitor.tickEnd(12.5);
        monitor.tickStart();
        monitor.tickEnd(75.25);

        assertEquals(List.of(0, 1), ticks);
        assertEquals(List.of(12.5, 75.25), durations);
        assertEquals(2, monitor.getCurrentTick());
        monitor.close();
    }

    @Test
    void ignoresTicksBeforeStartingAndAfterClosing() {
        SparkTickMonitor monitor = new SparkTickMonitor(Thread.currentThread());
        List<Integer> ticks = new ArrayList<>();
        List<Double> durations = new ArrayList<>();
        monitor.addCallback((TickHook.Callback) ticks::add);
        monitor.addCallback((TickReporter.Callback) durations::add);

        monitor.tickStart();
        monitor.tickEnd(10);
        assertEquals(0, monitor.getCurrentTick());
        assertTrue(ticks.isEmpty());
        assertTrue(durations.isEmpty());

        monitor.start();
        monitor.tickStart();
        monitor.tickEnd(15);
        monitor.close();
        monitor.close();
        monitor.tickStart();
        monitor.tickEnd(20);

        assertEquals(List.of(0), ticks);
        assertEquals(List.of(15d), durations);
        assertEquals(1, monitor.getCurrentTick());
    }

    @Test
    void removesCallbacksAndDoesNotDuplicateRegistrations() {
        SparkTickMonitor monitor = new SparkTickMonitor(Thread.currentThread());
        List<Integer> ticks = new ArrayList<>();
        List<Double> durations = new ArrayList<>();
        TickHook.Callback tickCallback = ticks::add;
        TickReporter.Callback durationCallback = durations::add;
        monitor.addCallback(tickCallback);
        monitor.addCallback(tickCallback);
        monitor.addCallback(durationCallback);

        monitor.start();
        monitor.tickStart();
        monitor.tickEnd(5);
        monitor.removeCallback(tickCallback);
        monitor.removeCallback(durationCallback);
        monitor.tickStart();
        monitor.tickEnd(6);
        monitor.close();

        assertEquals(List.of(0), ticks);
        assertEquals(List.of(5d), durations);
    }

    @Test
    void publishesTheNextIndexAfterCallbacksComplete() {
        SparkTickMonitor monitor = new SparkTickMonitor(Thread.currentThread());
        monitor.addCallback((TickHook.Callback) tick -> {
            int observed = CompletableFuture.supplyAsync(monitor::getCurrentTick).join();
            assertEquals(tick, observed);
        });

        monitor.start();
        monitor.tickStart();
        monitor.tickStart();

        assertEquals(Integer.valueOf(2), CompletableFuture.supplyAsync(monitor::getCurrentTick).join());
        monitor.close();
    }

    @Test
    void rejectsTickDeliveryFromOtherThreadsWithoutAdvancingTheCounter() {
        SparkTickMonitor monitor = new SparkTickMonitor(Thread.currentThread());
        monitor.start();

        CompletionException startFailure = assertThrows(CompletionException.class, () -> CompletableFuture.runAsync(monitor::tickStart).join());
        CompletionException endFailure = assertThrows(CompletionException.class, () -> CompletableFuture.runAsync(() -> monitor.tickEnd(1)).join());

        assertInstanceOf(IllegalStateException.class, startFailure.getCause());
        assertInstanceOf(IllegalStateException.class, endFailure.getCause());
        assertEquals(0, monitor.getCurrentTick());

        monitor.tickStart();
        assertEquals(1, monitor.getCurrentTick());
        monitor.close();
    }
}
