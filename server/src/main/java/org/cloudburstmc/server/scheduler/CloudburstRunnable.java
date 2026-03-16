package org.cloudburstmc.server.scheduler;

import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.scheduler.ScheduledTask;
import org.cloudburstmc.server.CloudServer;

/**
 * Convenience base class for scheduling tasks through the server scheduler.
 * Subclass this and implement {@link Runnable#run()} to define the task body.
 * <p>
 * All schedule methods delegate to the appropriate scheduler on {@link CloudServer}.
 * A single instance may only be scheduled once.
 */
public abstract class CloudburstRunnable implements Runnable {

    private ScheduledTask task;

    /**
     * Returns true if this task has been canceled.
     *
     * @return true if canceled
     * @throws IllegalStateException if the task has not been scheduled yet
     */
    public synchronized boolean isCancelled() {
        checkScheduled();
        return task.isCancelled();
    }

    /**
     * Cancels this task.
     *
     * @throws IllegalStateException if the task has not been scheduled yet
     */
    public synchronized void cancel() {
        checkScheduled();
        task.cancel();
    }

    /**
     * Returns the {@link ScheduledTask} handle assigned when this runnable was scheduled.
     *
     * @return the task handle
     * @throws IllegalStateException if the task has not been scheduled yet
     */
    public synchronized ScheduledTask getTask() {
        checkScheduled();
        return task;
    }

    /**
     * Schedules this task to run on the next server tick.
     *
     * @param plugin the owning plugin
     * @return the scheduled task handle
     * @throws IllegalStateException if already scheduled
     */
    public synchronized ScheduledTask runTask(PluginContainer plugin) {
        checkNotYetScheduled();
        return setup(CloudServer.getInstance().getGlobalScheduler().run(plugin, t -> this.run()));
    }

    /**
     * Schedules this task to run asynchronously as soon as possible.
     *
     * @param plugin the owning plugin
     * @return the scheduled task handle
     * @throws IllegalStateException if already scheduled
     */
    public synchronized ScheduledTask runTaskAsynchronously(PluginContainer plugin) {
        checkNotYetScheduled();
        return setup(CloudServer.getInstance().getAsyncScheduler().runNow(plugin, t -> this.run()));
    }

    /**
     * Schedules this task to run after the specified number of ticks.
     *
     * @param plugin the owning plugin
     * @param delay  ticks to wait before execution (must be &gt; 0)
     * @return the scheduled task handle
     * @throws IllegalStateException if already scheduled
     */
    public synchronized ScheduledTask runTaskLater(PluginContainer plugin, long delay) {
        checkNotYetScheduled();
        return setup(CloudServer.getInstance().getGlobalScheduler().runDelayed(plugin, t -> this.run(), delay));
    }

    /**
     * Schedules this task to run repeatedly on the main thread.
     *
     * @param plugin      the owning plugin
     * @param initialDelay ticks before first execution (must be &gt; 0)
     * @param period      ticks between executions (must be &gt; 0)
     * @return the scheduled task handle
     * @throws IllegalStateException if already scheduled
     */
    public synchronized ScheduledTask runTaskTimer(PluginContainer plugin, long initialDelay, long period) {
        checkNotYetScheduled();
        return setup(CloudServer.getInstance().getGlobalScheduler()
                .runAtFixedRate(plugin, t -> this.run(), initialDelay, period));
    }

    private void checkScheduled() {
        if (task == null) {
            throw new IllegalStateException("Not scheduled yet");
        }
    }

    private void checkNotYetScheduled() {
        if (task != null) {
            throw new IllegalStateException("Already scheduled");
        }
    }

    private ScheduledTask setup(ScheduledTask handle) {
        this.task = handle;
        return handle;
    }
}
