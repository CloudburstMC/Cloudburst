package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.level.gamerule.GameRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public interface GameRuleRegistry {

    <T extends Comparable<T>> void register(GameRule<T> gameRule);

    @Nullable
    GameRule<? extends Comparable<?>> fromString(String name);

    @Nonnull
    List<GameRule<?>> getRules();

    @Nonnull
    Set<String> getRuleNames();
}
