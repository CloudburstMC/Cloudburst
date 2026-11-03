package org.cloudburstmc.server.scheduler;

import org.cloudburstmc.api.scheduler.ScheduledTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CloudGlobalSchedulerTest {

    @Test
    void defersTasksSubmittedDuringTheCurrentTick() {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        List<Integer> calls = new ArrayList<>();
        scheduler.execute(null, () -> {
            calls.add(1);
            scheduler.execute(null, () -> calls.add(2));
        });

        assertTrue(calls.isEmpty());
        scheduler.tick();
        assertEquals(List.of(1), calls);

        scheduler.tick();
        assertEquals(List.of(1, 2), calls);
    }

    @Test
    void keepsDelayedAndRepeatingTasksOnSimulationTicks() {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        AtomicInteger tick = new AtomicInteger();
        List<Integer> calls = new ArrayList<>();
        scheduler.runAtFixedRate(null, task -> {
            calls.add(tick.get());
            if (calls.size() == 3) {
                task.cancel();
            }
        }, 2, 3);

        for (int current = 1; current <= 10; current++) {
            tick.set(current);
            scheduler.tick();
        }

        assertEquals(List.of(2, 5, 8), calls);
    }

    @Test
    void retainsDeadlinesBeyondTheIntegerTickRange() throws ReflectiveOperationException {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        Field tickCount = CloudGlobalScheduler.class.getDeclaredField("tickCount");
        tickCount.setAccessible(true);
        tickCount.setLong(scheduler, Integer.MAX_VALUE - 1L);
        AtomicInteger calls = new AtomicInteger();
        scheduler.runDelayed(null, task -> calls.incrementAndGet(), 3);

        scheduler.tick();
        scheduler.tick();
        assertEquals(0, calls.get());

        scheduler.tick();
        assertEquals(1, calls.get());
    }

    @Test
    void cancellationDoesNotExecutePendingCallbacksOrAdvanceTime() {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        AtomicInteger calls = new AtomicInteger();
        scheduler.execute(null, calls::incrementAndGet);
        scheduler.cancelAllTasks();
        scheduler.tick();

        assertEquals(0, calls.get());
    }

    @Test
    void finishesCancellationAfterTheRepeatingCallbackReturns() {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        AtomicInteger calls = new AtomicInteger();
        List<ScheduledTask.ExecutionState> states = new ArrayList<>();
        List<ScheduledTask.CancelledState> cancellations = new ArrayList<>();
        ScheduledTask scheduled = scheduler.runAtFixedRate(null, task -> {
            calls.incrementAndGet();
            states.add(task.getExecutionState());
            cancellations.add(CompletableFuture.supplyAsync(task::cancel).join());
            states.add(task.getExecutionState());
        }, 1, 1);

        scheduler.tick();

        assertEquals(1, calls.get());
        assertEquals(List.of(ScheduledTask.ExecutionState.RUNNING,
                ScheduledTask.ExecutionState.CANCELLED_RUNNING), states);
        assertEquals(List.of(ScheduledTask.CancelledState.NEXT_RUNS_CANCELLED), cancellations);
        assertEquals(ScheduledTask.ExecutionState.CANCELLED, scheduled.getExecutionState());
        assertEquals(ScheduledTask.CancelledState.CANCELLED_ALREADY, scheduled.cancel());

        scheduler.tick();
        assertEquals(1, calls.get());
    }

    @Test
    void runningOneShotFinishesInsteadOfBeingCancelled() {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        List<ScheduledTask.ExecutionState> states = new ArrayList<>();
        List<ScheduledTask.CancelledState> cancellations = new ArrayList<>();
        ScheduledTask scheduled = scheduler.run(null, task -> {
            cancellations.add(CompletableFuture.supplyAsync(task::cancel).join());
            states.add(task.getExecutionState());
        });

        scheduler.tick();

        assertEquals(List.of(ScheduledTask.CancelledState.RUNNING), cancellations);
        assertEquals(List.of(ScheduledTask.ExecutionState.RUNNING), states);
        assertEquals(ScheduledTask.ExecutionState.FINISHED, scheduled.getExecutionState());
        assertEquals(ScheduledTask.CancelledState.ALREADY_EXECUTED, scheduled.cancel());
    }

    @Test
    void rejectsOffThreadTicks() {
        CloudGlobalScheduler scheduler = new CloudGlobalScheduler();
        CompletionException failure = assertThrows(CompletionException.class,
                () -> CompletableFuture.runAsync(scheduler::tick).join());

        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }
}
