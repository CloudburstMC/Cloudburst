package org.cloudburstmc.server.registry;

import javax.annotation.Nonnull;

public class RegistryProvider<T> implements Comparable<RegistryProvider<T>> {
    private final T value;
    private final Object plugin;
    private final int priority;

    RegistryProvider(T value, Object plugin, int priority) {
        this.value = value;
        this.plugin = plugin;
        this.priority = priority;
    }

    @Nonnull
    public T getValue() {
        return value;
    }

    public Object getPlugin() {
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
