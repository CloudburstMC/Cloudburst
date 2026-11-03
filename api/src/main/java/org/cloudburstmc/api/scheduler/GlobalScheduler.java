package org.cloudburstmc.api.scheduler;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.function.Consumer;

/**
 * Schedules tasks on the main server thread using simulation tick delays.
 */
public interface GlobalScheduler {

    /**
     * Schedules a runnable to execute on the next tick.
     *
     * @param plugin the owning plugin
     * @param run    task body
     */
    void execute(PluginContainer plugin, Runnable run);

    /**
     * Schedules a task to execute on the next tick.
     *
     * @param plugin the owning plugin
     * @param task   callback receiving the {@link ScheduledTask} handle
     * @return the scheduled task handle
     */
    ScheduledTask run(PluginContainer plugin, Consumer<ScheduledTask> task);

    /**
     * Schedules a task to execute after {@code delayTicks} ticks.
     *
     * @param plugin     the owning plugin
     * @param task       callback receiving the {@link ScheduledTask} handle
     * @param delayTicks ticks to wait before first execution (must be &gt; 0)
     * @return the scheduled task handle
     */
    ScheduledTask runDelayed(PluginContainer plugin, Consumer<ScheduledTask> task, long delayTicks);

    /**
     * Schedules a repeating task.
     *
     * @param plugin            the owning plugin
     * @param task              callback receiving the {@link ScheduledTask} handle
     * @param initialDelayTicks ticks before first execution (must be &gt; 0)
     * @param periodTicks       ticks between subsequent executions (must be &gt; 0)
     * @return the scheduled task handle
     */
    ScheduledTask runAtFixedRate(PluginContainer plugin, Consumer<ScheduledTask> task,
                                long initialDelayTicks, long periodTicks);

    /**
     * Cancels all tasks owned by the given plugin.
     *
     * @param plugin the plugin whose tasks should be canceled
     */
    void cancelTasks(PluginContainer plugin);

    /**
     * Cancels all pending tasks across all plugins without executing their callbacks.
     */
    void cancelAllTasks();
}
