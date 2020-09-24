package org.cloudburstmc.server.scheduler;

import lombok.extern.log4j.Log4j2;

/**
 * Represents a task being executed by the {@link ServerScheduler}.
 *
 * For plugin developers: To make sure your task will only be executed in the case of safety
 * (such as: prevent this task from running if its owner plugin is disabled),
 * it's suggested to use {@link PluginTask} instead of extend this class.
 */
@Log4j2
public abstract class Task implements Runnable {
    private TaskHandler taskHandler = null;

    public final TaskHandler getHandler() {
        return this.taskHandler;
    }

    public final int getTaskId() {
        return this.taskHandler != null ? this.taskHandler.getTaskId() : -1;
    }

    public final void setHandler(TaskHandler taskHandler) {
        if (this.taskHandler == null || taskHandler == null) {
            this.taskHandler = taskHandler;
        }
    }

    /**
     * The method called when a task is executed.

     * @param currentTick The elapsed tick count from the server is started.
     *                    20 ticks = 1 second, 1 tick = 0.05 seconds
     */
    public abstract void onRun(int currentTick);

    @Override
    public final void run() {
        this.onRun(taskHandler.getLastRunTick());
    }

    public void onCancel() {

    }

    public void cancel() {
        try {
            this.getHandler().cancel();
        } catch (RuntimeException ex) {
            log.error("Exception while invoking onCancel", ex);
        }
    }

}
