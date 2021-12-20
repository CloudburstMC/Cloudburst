package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.function.BiFunction;

public interface BehaviorRegistry {

    <T, R> void registerBehavior(BehaviorKey<T, R> key, T defaultBehavior, BiFunction<Behavior<R>, T, R> executorFactory);

    <T> T getDefaultBehavior(BehaviorKey<T, ?> key);
}
