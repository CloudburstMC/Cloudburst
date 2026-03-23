package org.cloudburstmc.api.player;

import java.util.Set;

/**
 * Represents a player's current ability state, including boolean flags such as
 * whether they can fly, build, or take damage, and float speed values for walking
 * and flying.
 *
 * <p>Changes are not applied until {@link #update()} is called.</p>
 */
public interface PlayerAbilities {

    /**
     * Returns whether the given ability flag is currently active.
     *
     * @param ability the ability to query
     * @return {@code true} if the ability is active
     */
    boolean get(Ability ability);

    /**
     * Sets the given ability flag.
     *
     * @param ability the ability to set
     * @param value   {@code true} to enable, {@code false} to disable
     * @return {@code this} for chaining
     */
    PlayerAbilities set(Ability ability, boolean value);

    /**
     * Replaces the entire ability flag state with the provided set.
     * All abilities in {@code enabled} are set to {@code true}; all others are set to {@code false}.
     *
     * @param enabled the complete set of abilities that should be active
     * @return {@code this} for chaining
     */
    PlayerAbilities setAll(Set<Ability> enabled);

    /**
     * Returns the player's current walk speed.
     *
     * @return walk speed (default {@code 0.1})
     */
    float getWalkSpeed();

    /**
     * Sets the player's walk speed.
     *
     * @param speed the new walk speed
     * @return {@code this} for chaining
     */
    PlayerAbilities setWalkSpeed(float speed);

    /**
     * Returns the player's current horizontal fly speed.
     *
     * @return fly speed (default {@code 0.05})
     */
    float getFlySpeed();

    /**
     * Sets the player's horizontal fly speed.
     *
     * @param speed the new horizontal fly speed
     * @return {@code this} for chaining
     */
    PlayerAbilities setFlySpeed(float speed);

    /**
     * Returns the player's current vertical fly speed.
     *
     * @return vertical fly speed (default {@code 1.0})
     */
    float getVerticalFlySpeed();

    /**
     * Sets the player's vertical fly speed.
     *
     * @param speed the new vertical fly speed
     * @return {@code this} for chaining
     */
    PlayerAbilities setVerticalFlySpeed(float speed);

    /**
     * Applies any pending ability changes to the player.
     * Must be called after mutations to take effect.
     */
    void update();
}
