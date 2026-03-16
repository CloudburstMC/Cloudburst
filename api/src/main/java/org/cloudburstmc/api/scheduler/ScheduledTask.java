package org.cloudburstmc.api.scheduler;

import org.cloudburstmc.api.plugin.PluginContainer;

/**
 * Represents a task that has been submitted to a scheduler.
 */
public interface ScheduledTask {

    /**
     * Returns the plugin container that owns this task.
     *
     * @return the owning plugin container
     */
    PluginContainer getOwningPlugin();

    /**
     * Returns whether this task repeats on a fixed period.
     *
     * @return true if repeating
     */
    boolean isRepeatingTask();

    /**
     * Attempts to cancel this task.
     * If the task is currently executing, future executions are prevented but the current run is not interrupted.
     *
     * @return the result of the cancellation attempt
     */
    CancelledState cancel();

    /**
     * Returns the current execution state of this task.
     *
     * @return current execution state
     */
    ExecutionState getExecutionState();

    /**
     * Returns true if the task is canceled or cancellation-running.
     *
     * @return true if canceled
     */
    default boolean isCancelled() {
        ExecutionState state = this.getExecutionState();
        return state == ExecutionState.CANCELLED || state == ExecutionState.CANCELLED_RUNNING;
    }

    /**
     * Result of a cancellation attempt.
     */
    enum CancelledState {
        /** Task was not running and is now canceled. */
        CANCELLED_BY_CALLER,
        /** Task was already canceled before this call. */
        CANCELLED_ALREADY,
        /** Task is a one-shot task currently executing; cannot be canceled mid-run. */
        RUNNING,
        /** Task is a one-shot task that already finished; nothing to cancel. */
        ALREADY_EXECUTED,
        /** Repeating task is currently running; future runs canceled successfully. */
        NEXT_RUNS_CANCELLED,
        /** Repeating task is currently running; future runs were already canceled. */
        NEXT_RUNS_CANCELLED_ALREADY,
    }

    /**
     * Execution state of a scheduled task.
     */
    enum ExecutionState {
        /** Not yet running; may run in the future. */
        IDLE,
        /** Currently executing. */
        RUNNING,
        /** One-shot task that has finished normally. */
        FINISHED,
        /** Task will not execute (or will not execute again). */
        CANCELLED,
        /** Repeating task is executing its current run, but no future runs are scheduled. */
        CANCELLED_RUNNING,
    }
}
