package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.gamerule.GameRule;

import java.util.List;
import java.util.Set;

/**
 * Stores game-rule definitions available to levels and commands.
 *
 * <p>Registration is available until {@link #close()} is called. Rule names are matched
 * case-insensitively.</p>
 */
public interface GameRuleRegistry extends Registry {

    /**
     * Registers a game-rule definition.
     *
     * @param gameRule the definition to register
     * @param <T>      the rule value type
     */
    <T extends Comparable<T>> void register(GameRule<T> gameRule);

    /**
     * Finds a game rule by name.
     *
     * @param name the case-insensitive rule name
     * @return the rule, or {@code null} when no rule has that name
     */
    @Nullable
    GameRule<? extends Comparable<?>> fromString(String name);

    /**
     * Returns registered rules in registration order.
     *
     * @return immutable registered rules
     */
    List<GameRule<?>> getRules();

    /**
     * Returns registered rule names in registration order.
     *
     * @return immutable registered names
     */
    Set<String> getRuleNames();
}
