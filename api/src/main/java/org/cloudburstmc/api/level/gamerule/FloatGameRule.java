package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.Locale;
import java.util.Objects;

/**
 * An immutable floating-point game-rule definition.
 */
public final class FloatGameRule implements GameRule<Float> {
    private static final Class<Float> CLASS = Float.class;
    private final String name;
    private final Float defaultValue;
    private final boolean requiresCheats;

    private FloatGameRule(String name, float defaultValue, boolean requiresCheats) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.requiresCheats = requiresCheats;
    }

    /**
     * Creates a rule with a default value of {@code 0.0}.
     *
     * @param name the command and storage name
     * @return the game-rule definition
     */
    public static FloatGameRule of(String name) {
        return of(name, 0.0f);
    }

    /**
     * Creates a rule that is available without cheats.
     *
     * @param name         the command and storage name
     * @param defaultValue the initial value
     * @return the game-rule definition
     */
    public static FloatGameRule of(String name, float defaultValue) {
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
    public static FloatGameRule of(String name, float defaultValue, boolean requiresCheats) {
        return new FloatGameRule(name, defaultValue, requiresCheats);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Float> getValueClass() {
        return CLASS;
    }

    @Override
    public Float getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean requiresCheats() {
        return this.requiresCheats;
    }

    @Override
    public Float parse(String value) {
        return Float.parseFloat(value);
    }

    @Override
    public String serialize(Float value) {
        return value.toString();
    }

    @Override
    public CommandArgumentType<Float> argumentType() {
        return CommandArgumentTypes.floatingPoint();
    }

    @Override
    public int hashCode() {
        return Objects.hash(FloatGameRule.class, this.name.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof FloatGameRule that)) {
            return false;
        }

        return this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public String toString() {
        return "FloatGameRule(name=" + this.name + ")";
    }
}
