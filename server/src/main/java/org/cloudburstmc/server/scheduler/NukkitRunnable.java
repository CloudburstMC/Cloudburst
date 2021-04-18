package org.cloudburstmc.server.scheduler;

import org.cloudburstmc.server.CloudServer;

/**
 * This class is provided as an easy way to handle scheduling tasks.
 */
public abstract class NukkitRunnable implements Runnable {
    private TaskHandler taskHandler;

    /**
     * Attempts to cancel this task.
     *
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized void cancel() throws IllegalStateException {
        taskHandler.cancel();
    }

    public synchronized Runnable runTask(Object plugin) throws IllegalArgumentException, IllegalStateException {
        checkState();
        this.taskHandler = CloudServer.getInstance().getScheduler().scheduleTask(plugin, this);
        return taskHandler.getTask();
    }

    public synchronized Runnable runTaskAsynchronously(Object plugin) throws IllegalArgumentException, IllegalStateException {
        checkState();
        this.taskHandler = CloudServer.getInstance().getScheduler().scheduleTask(plugin, this, true);
        return taskHandler.getTask();
    }

    public synchronized Runnable runTaskLater(Object plugin, int delay) throws IllegalArgumentException, IllegalStateException {
        checkState();
        this.taskHandler = CloudServer.getInstance().getScheduler().scheduleDelayedTask(plugin, this, delay);
        return taskHandler.getTask();
    }

    public synchronized Runnable runTaskLaterAsynchronously(Object plugin, int delay) throws IllegalArgumentException, IllegalStateException {
        checkState();
        this.taskHandler = CloudServer.getInstance().getScheduler().scheduleDelayedTask(plugin, this, delay, true);
        return taskHandler.getTask();
    }

    public synchronized Runnable runTaskTimer(Object plugin, int delay, int period) throws IllegalArgumentException, IllegalStateException {
        checkState();
        this.taskHandler = CloudServer.getInstance().getScheduler().scheduleDelayedRepeatingTask(plugin, this, delay, period);
        return taskHandler.getTask();
    }

    public synchronized Runnable runTaskTimerAsynchronously(Object plugin, int delay, int period) throws IllegalArgumentException, IllegalStateException {
        checkState();
        this.taskHandler = CloudServer.getInstance().getScheduler().scheduleDelayedRepeatingTask(plugin, this, delay, period, true);
        return taskHandler.getTask();
    }

    /**
     * Gets the task id for this runnable.
     *
     * @return the task id that this runnable was scheduled as
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized int getTaskId() throws IllegalStateException {
        if (taskHandler == null) {
            throw new IllegalStateException("Not scheduled yet");
        }
        final int id = taskHandler.getTaskId();
        return id;
    }

    private void checkState() {
        if (taskHandler != null) {
            throw new IllegalStateException("Already scheduled as " + taskHandler.getTaskId());
        }
    }
}