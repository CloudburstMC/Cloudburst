package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.gamerule.GameRule;

import java.util.List;
import java.util.Set;

public interface GameRuleRegistry extends Registry {

    <T extends Comparable<T>> void register(GameRule<T> gameRule);

    @Nullable
    GameRule<? extends Comparable<?>> fromString(String name);

    List<GameRule<?>> getRules();

    Set<String> getRuleNames();
}
