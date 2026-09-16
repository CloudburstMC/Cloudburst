package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.Locale;
import java.util.Objects;

/**
 * An immutable boolean-valued game-rule definition.
 */
public final class BooleanGameRule implements GameRule<Boolean> {
    private static final Class<Boolean> CLASS = Boolean.class;
    private final String name;
    private final Boolean defaultValue;
    private final boolean requiresCheats;

    private BooleanGameRule(String name, boolean defaultValue, boolean requiresCheats) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.requiresCheats = requiresCheats;
    }

    /**
     * Creates a rule with a default value of {@code false}.
     *
     * @param name the command and storage name
     * @return the game-rule definition
     */
    public static BooleanGameRule of(String name) {
        return of(name, false);
    }

    /**
     * Creates a rule that is available without cheats.
     *
     * @param name         the command and storage name
     * @param defaultValue the initial value
     * @return the game-rule definition
     */
    public static BooleanGameRule of(String name, boolean defaultValue) {
        return of(name, defaultValue, false);
    }

    /**
     * Creates a rule.
     *
     * @param name           the command and storage name
     * @param defaultValue   the initial value
     * @param requiresCheats whether changing the rule requires cheats
     * @return the game-rule definition
     */
    public static BooleanGameRule of(String name, boolean defaultValue, boolean requiresCheats) {
        return new BooleanGameRule(name, defaultValue, requiresCheats);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Boolean> getValueClass() {
        return CLASS;
    }

    @Override
    public Boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean requiresCheats() {
        return this.requiresCheats;
    }

    @Override
    public Boolean parse(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }

        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        throw new IllegalArgumentException("Invalid boolean value for gamerule " + name + ": " + value);
    }

    @Override
    public String serialize(Boolean value) {
        return value.toString();
    }

    @Override
    public CommandArgumentType<Boolean> argumentType() {
        return CommandArgumentTypes.bool();
    }

    @Override
    public int hashCode() {
        return Objects.hash(BooleanGameRule.class, this.name.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof BooleanGameRule that)) {
            return false;
        }

        return this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public String toString() {
        return "BooleanGameRule(name=" + this.name + ")";
    }
}
