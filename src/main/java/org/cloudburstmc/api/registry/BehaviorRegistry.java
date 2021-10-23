package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.function.Function;

public interface BehaviorRegistry extends Registry {

    <T, R> void registerItemBehavior(BehaviorKey<T, R> key, T defaultBehavior, Function<Behavior, R> executorFactory);

    <T, R> void registerBlockBehavior(BehaviorKey<T, R> key, T defaultBehavior, Function<Behavior, R> executorFactory);
}
