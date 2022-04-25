package org.cloudburstmc.server.registry.behavior;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.registry.BehaviorRegistry;
import org.cloudburstmc.api.util.behavior.BehaviorBuilder;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;
import org.cloudburstmc.server.registry.CloudBehaviorRegistry;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@RequiredArgsConstructor
public class CloudBehaviorCollection implements BehaviorCollection {

    private final Map<BehaviorKey<?, ?>, CloudBehavior<?>> behaviors = new IdentityHashMap<>();
    private final CloudBehaviorRegistry<?> registry;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(BehaviorKey<?, T> key) {
        CloudBehavior<T> behavior = (CloudBehavior<T>) behaviors.get(key);
        checkNotNull(behavior, "behaviour does not exist for '" + key.getId() + "'");
        return behavior.executor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T, R> BehaviorCollection extend(BehaviorKey<T, R> key, T function) {
        checkNotNull(key, "key");
        checkArgument(key.getType().isInstance(function), "function is not instance of %s", key.getType().getName());

        CloudBehavior<R> behavior = createBehavior(key, function, "extended");
        behavior.parent = (CloudBehavior<R>) this.behaviors.getOrDefault(key, DefaultCloudBehavior.get());
        this.behaviors.put(key, behavior);

        return this;
    }

    @Override
    public synchronized <T, R> BehaviorCollection overwrite(BehaviorKey<T, R> key, T function) {
        checkNotNull(key, "key");
        checkArgument(key.getType().isInstance(function), "function is not instance of %s", key.getType().getName());

        CloudBehavior<R> behavior = createBehavior(key, function, "overwritten");
        this.behaviors.put(key, behavior);

        return this;
    }

    @Override
    public void apply(BehaviorBuilder builder) {
        checkNotNull(builder, "builder");
        builder.applyTo(this);
    }

    @Override
    public BehaviorRegistry<?> getRegistry() {
        return registry;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void bake() {
        // Replace fake default behaviours with real ones
        for (Map.Entry<BehaviorKey<?, ?>, CloudBehavior<?>> entry : this.behaviors.entrySet()) {
            bakeEntry((Map.Entry) entry);
        }

        // Create default behaviours for keys that are not in the map
        this.registry.forEach((key, defaultValue) -> {
            if (this.behaviors.containsKey(key)) {
                return;
            }

            Object function = this.registry.getDefaultBehavior(key);
            this.behaviors.put(key, createBehavior((BehaviorKey) key, function, "default"));
        });
    }

    private <T, R> void bakeEntry(Map.Entry<BehaviorKey<T, R>, CloudBehavior<R>> entry) {
        CloudBehavior<R> behavior = entry.getValue();

        while (behavior.parent != null && behavior.parent != DefaultCloudBehavior.get()) {
            behavior = behavior.parent;
        }

        if (behavior.parent == null) {
            return; // Ignore overwritten behaviours
        }

        T function = this.registry.getDefaultBehavior(entry.getKey());
        behavior.parent = createBehavior(entry.getKey(), function, "default");
    }

    private <T, R> CloudBehavior<R> createBehavior(BehaviorKey<T, R> key, T function, String name) {
        CloudBehavior<R> behavior = new CloudBehavior<>(this, name);
        behavior.executor = this.registry.createExecutor(key, behavior, function);
        return behavior;
    }
}
