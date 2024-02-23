package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;

public interface BehaviorRegistry<T> extends Registry<T> {

    <F> void registerBehavior(BehaviorKey<F, F> key, F defaultBehavior);

    <F, E> void registerContextBehavior(BehaviorKey<F, E> key, F defaultBehavior);

    <F> F getDefaultBehavior(BehaviorKey<F, ?> key);

    BehaviorCollection getBehaviors(T type);

    default <E> E getBehavior(T type, BehaviorKey<?, E> key) {
        return getBehaviors(type).get(key);
    }

    GlobalRegistry global();
}