package org.cloudburstmc.api.scheduler;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler for tasks that execute asynchronously, off the main server tick thread.
 * Delays and periods are specified in real wall-clock time via {@link TimeUnit}.
 */
public interface AsyncScheduler {

    /**
     * Schedules a task to execute asynchronously as soon as possible.
     *
     * @param plugin the owning plugin
     * @param task   callback receiving the {@link ScheduledTask} handle
     * @return the scheduled task handle
     */
    ScheduledTask runNow(PluginContainer plugin, Consumer<ScheduledTask> task);

    /**
     * Schedules a task to execute asynchronously after a real-time delay.
     *
     * @param plugin the owning plugin
     * @param task   callback receiving the {@link ScheduledTask} handle
     * @param delay  delay before execution (must be &ge; 0)
     * @param unit   time unit for the delay
     * @return the scheduled task handle
     */
    ScheduledTask runDelayed(PluginContainer plugin, Consumer<ScheduledTask> task, long delay, TimeUnit unit);

    /**
     * Schedules a repeating async task with an initial delay and a fixed period.
     *
     * @param plugin        the owning plugin
     * @param task          callback receiving the {@link ScheduledTask} handle
     * @param initialDelay  delay before first execution (must be &ge; 0)
     * @param period        period between executions (must be &gt; 0)
     * @param unit          time unit for both delay and period
     * @return the scheduled task handle
     */
    ScheduledTask runAtFixedRate(PluginContainer plugin, Consumer<ScheduledTask> task, long initialDelay, long period, TimeUnit unit);

    /**
     * Cancels all tasks owned by the given plugin.
     *
     * @param plugin the plugin whose tasks should be canceled
     */
    void cancelTasks(PluginContainer plugin);

    /**
     * Cancels all async tasks across all plugins.
     * Called during server shutdown.
     */
    void cancelAllTasks();
}
