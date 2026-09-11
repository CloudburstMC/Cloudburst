package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.Locale;
import java.util.Objects;

public final class BooleanGameRule implements GameRule<Boolean> {
    private static final Class<Boolean> CLASS = Boolean.class;
    private final String name;
    private final Boolean defaultValue;

    private BooleanGameRule(String name, boolean defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static BooleanGameRule of(String name) {
        return of(name, false);
    }

    public static BooleanGameRule of(String name, boolean defaultValue) {
        return new BooleanGameRule(name, defaultValue);
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
