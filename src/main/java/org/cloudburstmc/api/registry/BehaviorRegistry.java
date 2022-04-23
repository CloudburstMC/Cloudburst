package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.util.behavior.Behavior;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;

import java.util.function.BiFunction;

public interface BehaviorRegistry<T> {

    <F, E> void registerBehavior(BehaviorKey<F, E> key, F defaultBehavior, BiFunction<Behavior<E>, F, E> executorFactory);

    <F> F getDefaultBehavior(BehaviorKey<F, ?> key);

    BehaviorCollection getBehaviors(T type);

    default <E> E getBehavior(T type, BehaviorKey<?, E> key) {
        return getBehaviors(type).get(key);
    }

    GlobalRegistry global();
}
