package org.cloudburstmc.server.level.light;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Advances submitted light slices deterministically without starting a worker pool.
 */
public class LightTestTasks implements Function<Runnable, CompletableFuture<Void>> {

    private final BlockingQueue<Submission> pending = new LinkedBlockingQueue<>();

    @Override
    public CompletableFuture<Void> apply(Runnable task) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        this.pending.add(new Submission(task, completion));
        return completion;
    }

    public void runNext() {
        Submission next;

        try {
            next = this.pending.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for a light task", interrupted);
        }

        if (next == null) {
            throw new AssertionError("No light task was submitted");
        }

        try {
            next.task.run();
            next.completion.complete(null);
        } catch (Throwable error) {
            next.completion.completeExceptionally(error);
        }
    }

    public void drain(LightScheduler scheduler) {
        for (int slice = 0; slice < 100_000; slice++) {
            if (scheduler.isIdle()) {
                scheduler.checkFailure();
                return;
            }

            this.runNext();
        }

        throw new AssertionError("Lighting did not drain within the slice budget");
    }

    public static void drain(LightRegion.Operation operation) {
        for (int slice = 0; slice < 100_000; slice++) {
            if (operation.advance() == LightRegion.Step.COMPLETE) {
                return;
            }
        }

        throw new AssertionError("Light operation did not drain within the slice budget");
    }

    private record Submission(Runnable task, CompletableFuture<Void> completion) {
    }
}
