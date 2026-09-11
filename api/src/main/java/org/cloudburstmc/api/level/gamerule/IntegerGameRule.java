package org.cloudburstmc.api.level.gamerule;

import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.Locale;
import java.util.Objects;

public final class IntegerGameRule implements GameRule<Integer> {
    private static final Class<Integer> CLASS = Integer.class;
    private final String name;
    private final Integer defaultValue;

    private IntegerGameRule(String name, int defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static IntegerGameRule of(String name) {
        return of(name, 0);
    }

    public static IntegerGameRule of(String name, int defaultValue) {
        return new IntegerGameRule(name, defaultValue);
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
