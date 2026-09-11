package org.cloudburstmc.api.level.gamerule;

import java.util.Collection;
import java.util.Optional;

/**
 * Typed game-rule values for a level.
 */
public interface LevelGameRules extends Iterable<LevelGameRules.Entry<?>> {

    /**
     * Checks whether this level has a value for a game rule.
     *
     * @param gameRule the game rule
     * @return {@code true} when the rule has a value
     */
    boolean contains(GameRule<?> gameRule);

    /**
     * Gets the current value for a game rule.
     *
     * @param gameRule the game rule
     * @param <T>      the game-rule value type
     * @return the current value
     * @throws java.util.NoSuchElementException if the rule has no value
     */
    <T extends Comparable<T>> T get(GameRule<T> gameRule);

    /**
     * Gets the current value for a game rule if it is present.
     *
     * @param gameRule the game rule
     * @param <T>      the game-rule value type
     * @return the current value, or an empty optional if the rule has no value
     */
    <T extends Comparable<T>> Optional<T> find(GameRule<T> gameRule);

    /**
     * Sets a game-rule value.
     *
     * @param gameRule the game rule
     * @param value    the new value
     * @param <T>      the game-rule value type
     * @return {@code true} when the stored value changed
     */
    <T extends Comparable<T>> boolean set(GameRule<T> gameRule, T value);

    /**
     * Gets the game rules with values in this level.
     *
     * @return the present game rules
     */
    Collection<GameRule<?>> rules();

    /**
     * Gets the number of game rules with values in this level.
     *
     * @return the number of present game rules
     */
    int size();

    /**
     * An immutable typed game-rule entry.
     *
     * @param rule  the game rule
     * @param value the current value
     * @param <T>   the game-rule value type
     */
    record Entry<T extends Comparable<T>>(GameRule<T> rule, T value) {
    }
}
