package org.cloudburstmc.server.command.argument;

/**
 * Describes the root form of a target selector.
 */
public enum CloudSelectorKind {
    /**
     * A literal player or entity name.
     */
    NAME,
    /**
     * The nearest player, represented by {@code @p}.
     */
    NEAREST_PLAYER,
    /**
     * All online players, represented by {@code @a}.
     */
    ALL_PLAYERS,
    /**
     * A random online player, represented by {@code @r}.
     */
    RANDOM_PLAYER,
    /**
     * The command executor, represented by {@code @s}.
     */
    SELF,
    /**
     * All entities, represented by {@code @e}.
     */
    ALL_ENTITIES,
    /**
     * The nearest non-player entity, represented by {@code @n}.
     */
    NEAREST_ENTITY
}
