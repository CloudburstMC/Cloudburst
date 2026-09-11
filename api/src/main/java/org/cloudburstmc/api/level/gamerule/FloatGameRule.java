package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.Locale;
import java.util.Objects;

public final class FloatGameRule implements GameRule<Float> {
    private static final Class<Float> CLASS = Float.class;
    private final String name;
    private final Float defaultValue;

    private FloatGameRule(String name, float defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static FloatGameRule of(String name) {
        return of(name, 0.0f);
    }

    public static FloatGameRule of(String name, float defaultValue) {
        return new FloatGameRule(name, defaultValue);
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
