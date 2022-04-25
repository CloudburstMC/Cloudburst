package org.cloudburstmc.server.registry.behavior;

import org.cloudburstmc.api.data.BehaviorKey;

public class DefaultCloudBehavior<T> extends CloudBehavior<T> {

    private static final DefaultCloudBehavior<?> INSTANCE = new DefaultCloudBehavior<>();

    @SuppressWarnings("unchecked")
    public static <T> CloudBehavior<T> get() {
        return (CloudBehavior<T>) INSTANCE;
    }

    private DefaultCloudBehavior() {
        super(null, "default");
    }

    @Override
    public <U> U get(BehaviorKey<?, U> key) {
        throw new UnsupportedOperationException("Cannot be called whilst the registry is open");
    }
}
