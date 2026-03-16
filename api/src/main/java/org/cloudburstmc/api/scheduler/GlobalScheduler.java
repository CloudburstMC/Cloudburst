package org.cloudburstmc.api.scheduler;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.function.Consumer;

/**
 * Scheduler for tasks that execute on the main server thread, bound to the server tick.
 * Tasks submitted here are guaranteed to run on the same thread that calls {@link #tick(int)}.
 */
public interface GlobalScheduler {

    /**
     * Schedules a runnable to execute on the next tick. Fire-and-forget: no handle returned.
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
    ScheduledTask runAtFixedRate(PluginContainer plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    /**
     * Cancels all tasks owned by the given plugin.
     *
     * @param plugin the plugin whose tasks should be canceled
     */
    void cancelTasks(PluginContainer plugin);

    /**
     * Cancels all pending tasks across all plugins and drains the queue.
     * Called during server shutdown.
     */
    void cancelAllTasks();

    /**
     * Advances the scheduler by one tick, executing all tasks whose deadline has been reached.
     * Must be called from the main server thread.
     *
     * @param currentTick the current server tick counter
     */
    void tick(int currentTick);
}
