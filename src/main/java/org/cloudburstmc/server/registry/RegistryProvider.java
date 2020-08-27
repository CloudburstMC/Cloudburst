package org.cloudburstmc.server.registry;

import org.cloudburstmc.server.plugin.PluginContainer;

import javax.annotation.Nonnull;

public class RegistryProvider<T> implements Comparable<RegistryProvider<T>> {
    private final T value;
    private final PluginContainer plugin;
    private final int priority;

    RegistryProvider(T value, PluginContainer plugin, int priority) {
        this.value = value;
        this.plugin = plugin;
        this.priority = priority;
    }

    @Nonnull
    public T getValue() {
        return value;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(RegistryProvider<T> that) {
        return Integer.compare(this.priority, that.priority);
    }
}
