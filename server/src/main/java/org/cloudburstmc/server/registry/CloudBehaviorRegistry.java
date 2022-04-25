package org.cloudburstmc.server.registry;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.registry.BehaviorRegistry;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.*;

public abstract class CloudBehaviorRegistry<T> implements BehaviorRegistry<T>, Registry {

    protected final Map<BehaviorKey<?, ?>, Data<?, ?>> behaviors = new HashMap<>();

    @Override
    public <F, E> void registerBehavior(BehaviorKey<F, E> key, F defaultBehavior, BiFunction<Behavior<E>, F, E> executorFactory) {
        checkNotNull(key, "key");
        checkArgument(key.getType().isInstance(defaultBehavior), "defaultBehavior is not instance of %", key.getType());
        checkNotNull(executorFactory, "executorFactory");
        checkState(!this.behaviors.containsKey(key), "%s has already been registered");

        this.behaviors.put(key, new Data<>(defaultBehavior, executorFactory));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F getDefaultBehavior(BehaviorKey<F, ?> key) {
        Data<F, ?> data = (Data<F, ?>) behaviors.get(key);

        return data == null ? null : data.defaultValue;
    }

    public boolean isBehaviorRegistered(BehaviorKey<?, ?> key) {
        return behaviors.containsKey(key);
    }

    public void forEach(BiConsumer<BehaviorKey<?, ?>, Object> consumer) {
        checkNotNull(consumer, "consumer");
        for (Map.Entry<BehaviorKey<?, ?>, Data<?, ?>> entry : this.behaviors.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue().defaultValue);
        }
    }

    @SuppressWarnings("unchecked")
    public <F, E> E createExecutor(BehaviorKey<F, E> key, Behavior<E> behavior, F function) {
        Data<F, E> data = (Data<F, E>) behaviors.get(key);
        checkArgument(data != null, "No behavior registered for %s", key);

        return data.executorFactory.apply(behavior, function);
    }

    @Value
    private static class Data<F, E> {
        @NonFinal F defaultValue;
        BiFunction<Behavior<E>, F, E> executorFactory;
    }
}
