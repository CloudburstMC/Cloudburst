package org.cloudburstmc.server.registry.behavior;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.registry.BehaviorRegistry;
import org.cloudburstmc.api.util.behavior.Behavior;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;

@RequiredArgsConstructor
public class CloudBehavior<T> implements Behavior<T> {

    private final BehaviorCollection collection;
    private final String name;
    T executor;
    CloudBehavior<T> parent;

    @Override
    public @NonNull T parent() {
        if (parent == null) {
            throw new IllegalCallerException("Behaviour '" + name + "' has no parent");
        }
        return parent.executor;
    }

    @Override
    public <U> U get(BehaviorKey<?, U> key) {
        return collection.get(key);
    }

    @Override
    public <U> BehaviorRegistry<U> getRegistry(Class<U> type) {
        return collection.getRegistry().global().getRegistry(type);
    }

    public T getExecutor() {
        return executor;
    }
}
