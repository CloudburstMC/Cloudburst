package org.cloudburstmc.server.scheduler;


import com.google.common.base.Preconditions;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;

import javax.annotation.Nonnull;

/**
 * Represents a task created by a plugin.
 *
 * For plugin developers: Tasks that extend this class, won't be executed when the plugin is disabled.
 *
 * Otherwise, tasks that extend this class can use {@link #getOwner()} to get its owner.
 *
 * An example for plugin create a task:
 * <pre>
 *     public class ExampleTask extends PluginTask&lt;ExamplePlugin&gt;{
 *         public ExampleTask(ExamplePlugin plugin){
 *             super(plugin);
 *         }
 *
 *        {@code @Override}
 *         public void onRun(int currentTick){
 *             getOwner().getLogger().info("Task is executed in tick "+currentTick);
 *         }
 *     }
 * </pre>
 *
 * <p>If you want Cloudburst to execute this task with delay or repeat, use {@link ServerScheduler}.</p>
 */
public abstract class PluginTask<T> extends Task {

    protected final T owner;
    protected final PluginContainer container;

    /**
     * Constructs a plugin-owned task.
     *
     * @param owner The plugin object that owns this task.
     */
    public PluginTask(@Nonnull T owner) {
        Preconditions.checkNotNull(owner, "owner");
        this.owner = owner;
        this.container = CloudServer.getInstance().getPluginManager().fromInstance(owner).orElseThrow(() ->
                new IllegalArgumentException("Object " + owner + " is not a plugin")
        );
    }

    /**
     * Returns the owner of this task.
     *
     * @return The plugin object that owns this task.
     */
    @Nonnull
    public final T getOwner() {
        return this.owner;
    }

    @Nonnull
    public PluginContainer getContainer() {
        return container;
    }
}
