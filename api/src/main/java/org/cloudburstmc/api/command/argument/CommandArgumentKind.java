package org.cloudburstmc.api.command.argument;

/**
 * Describes how a command argument should be parsed and presented in command UI.
 */
public enum CommandArgumentKind {
    /**
     * A single string token.
     */
    STRING,
    /**
     * The remaining command input as free-form text.
     */
    TEXT,
    /**
     * The remaining command input as a chat message.
     */
    MESSAGE,
    /**
     * A signed integer.
     */
    INTEGER,
    /**
     * A floating-point number.
     */
    FLOAT,
    /**
     * An absolute or relative rotation value.
     */
    ROTATION,
    /**
     * A player or entity selector.
     */
    TARGET,
    /**
     * A three-axis position.
     */
    POSITION,
    /**
     * A three-axis block position.
     */
    BLOCK_POSITION,
    /**
     * The remaining command input as JSON text.
     */
    JSON,
    /**
     * An item identifier from the item registry.
     */
    ITEM,
    /**
     * A block identifier from the block registry.
     */
    BLOCK,
    /**
     * An enchantment identifier from the enchantment registry.
     */
    ENCHANTMENT,
    /**
     * An effect identifier from the effect registry.
     */
    EFFECT,
    /**
     * An entity type identifier from the entity type registry.
     */
    ENTITY_TYPE,
    /**
     * A particle identifier from the particle registry.
     */
    PARTICLE,
    /**
     * A fixed set of literal values.
     */
    FIXED_ENUM,
    /**
     * A numeric argument with a required suffix.
     */
    POSTFIX
}
