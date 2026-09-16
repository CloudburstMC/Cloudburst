package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.Locale;
import java.util.Objects;

/**
 * An immutable integer-valued game-rule definition.
 */
public final class IntegerGameRule implements GameRule<Integer> {
    private static final Class<Integer> CLASS = Integer.class;
    private final String name;
    private final Integer defaultValue;
    private final boolean requiresCheats;

    private IntegerGameRule(String name, int defaultValue, boolean requiresCheats) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.requiresCheats = requiresCheats;
    }

    /**
     * Creates a rule with a default value of {@code 0}.
     *
     * @param name the command and storage name
     * @return the game-rule definition
     */
    public static IntegerGameRule of(String name) {
        return of(name, 0);
    }

    /**
     * Creates a rule that is available without cheats.
     *
     * @param name         the command and storage name
     * @param defaultValue the initial value
     * @return the game-rule definition
     */
    public static IntegerGameRule of(String name, int defaultValue) {
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
    public static IntegerGameRule of(String name, int defaultValue, boolean requiresCheats) {
        return new IntegerGameRule(name, defaultValue, requiresCheats);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Integer> getValueClass() {
        return CLASS;
    }

    @Override
    public Integer getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean requiresCheats() {
        return this.requiresCheats;
    }

    @Override
    public Integer parse(String value) {
        return Integer.parseInt(value);
    }

    @Override
    public String serialize(Integer value) {
        return value.toString();
    }

    @Override
    public CommandArgumentType<Integer> argumentType() {
        return CommandArgumentTypes.integer();
    }

    @Override
    public int hashCode() {
        return Objects.hash(IntegerGameRule.class, this.name.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof IntegerGameRule that)) {
            return false;
        }

        return this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public String toString() {
        return "IntegerGameRule(name=" + this.name + ")";
    }
}
