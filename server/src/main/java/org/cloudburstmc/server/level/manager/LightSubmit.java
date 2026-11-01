package org.cloudburstmc.server.level.manager;

import com.google.common.base.Preconditions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * Hands light work to the generation queue for the light lane.
 *
 * <p>A rejection that arrives synchronously is rethrown so the lane can park
 * itself. A rejection that arrives asynchronously, after the work was accepted,
 * completes the future with no observer and leaves that level's lane parked;
 * the queue only rejects queued work while the executors are shutting down, so
 * this only happens during shutdown.
 */
public final class LightSubmit implements Consumer<Runnable> {

    private final ChunkTaskScheduler scheduler;

    /**
     * @param scheduler the level's chunk task scheduler
     */
    public LightSubmit(ChunkTaskScheduler scheduler) {
        this.scheduler = Preconditions.checkNotNull(scheduler, "scheduler");
    }

    @Override
    public void accept(Runnable work) {
        rethrowIfRejected(this.scheduler.scheduleLight(work));
    }

    /**
     * Rethrows a synchronously rejected light submission for the light lane,
     * ignoring any other outcome.
     *
     * @param future the submission's future
     */
    private static void rethrowIfRejected(CompletableFuture<Void> future) {
        if (future.isCompletedExceptionally()) {
            Throwable failure = future.exceptionNow();
            if (failure instanceof RejectedExecutionException rejected) {
                throw rejected;
            }
        }
    }
}
