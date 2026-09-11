package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;

public interface GameRule<T extends Comparable<T>> {

    /**
     * Gets the command and storage name of this rule.
     *
     * @return the rule name
     */
    String getName();

    /**
     * Gets the Java type accepted by this rule.
     *
     * @return the value class
     */
    Class<T> getValueClass();

    /**
     * Gets the value used when a level first creates this rule.
     *
     * @return the default value
     */
    T getDefaultValue();

    /**
     * Parses a value from persisted text.
     *
     * @param value the serialized value
     * @return the parsed value
     */
    T parse(String value);

    /**
     * Converts a rule value to command and storage text.
     *
     * @param value the value to serialize
     * @return the serialized value
     */
    String serialize(T value);

    /**
     * Creates the command argument used when changing this rule.
     *
     * @return the value argument type
     */
    CommandArgumentType<T> argumentType();
}
