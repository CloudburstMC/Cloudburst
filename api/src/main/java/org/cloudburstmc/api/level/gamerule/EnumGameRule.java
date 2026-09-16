package org.cloudburstmc.api.level.gamerule;

import lombok.Getter;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;

import java.util.*;

/**
 * An immutable game-rule definition whose named values are backed by enum ordinals.
 *
 * <p>The enum declaration order defines persisted integer values. Reordering constants after a rule has
 * been stored changes their meaning.</p>
 *
 * @param <E> the enum containing the rule's named values
 */
public final class EnumGameRule<E extends Enum<E>> implements GameRule<Integer> {

    private static final Class<Integer> CLASS = Integer.class;

    private final String name;
    @Getter
    private final Class<E> enumClass;
    private final Integer defaultValue;
    private final Map<String, Integer> values;
    private final boolean requiresCheats;

    private EnumGameRule(String name, Class<E> enumClass, int defaultValue, Map<String, Integer> values, boolean requiresCheats) {
        this.name = name;
        this.enumClass = enumClass;
        this.defaultValue = defaultValue;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.requiresCheats = requiresCheats;
    }

    /**
     * Creates a rule using every enum constant and the supplied default.
     *
     * @param name         the command and storage name
     * @param enumClass    the enum containing accepted values
     * @param defaultValue the initial value
     * @param <E>          the enum type
     * @return the game-rule definition
     */
    public static <E extends Enum<E>> EnumGameRule<E> of(String name, Class<E> enumClass, E defaultValue) {
        return of(name, enumClass, defaultValue, enumClass.getEnumConstants());
    }

    /**
     * Creates a rule using a restricted set of enum constants.
     *
     * @param name          the command and storage name
     * @param enumClass     the enum containing accepted values
     * @param defaultValue  the initial value
     * @param allowedValues the accepted values in advertised order
     * @param <E>           the enum type
     * @return the game-rule definition
     */
    @SafeVarargs
    public static <E extends Enum<E>> EnumGameRule<E> of(String name, Class<E> enumClass, E defaultValue, E... allowedValues) {
        return of(name, enumClass, defaultValue, false, allowedValues);
    }

    /**
     * Creates a rule using a restricted set of enum constants.
     *
     * @param name           the command and storage name
     * @param enumClass      the enum containing accepted values
     * @param defaultValue   the initial value
     * @param requiresCheats whether changing the rule requires cheats
     * @param allowedValues  the accepted values in advertised order
     * @param <E>            the enum type
     * @return the game-rule definition
     */
    @SafeVarargs
    public static <E extends Enum<E>> EnumGameRule<E> of(String name, Class<E> enumClass, E defaultValue,
                                                         boolean requiresCheats, E... allowedValues) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (E value : allowedValues) {
            values.put(value.name().toLowerCase(Locale.ROOT), value.ordinal());
        }
        return new EnumGameRule<>(name, enumClass, defaultValue.ordinal(), values, requiresCheats);
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

    public Set<String> getValues() {
        return this.values.keySet();
    }

    @Override
    public String serialize(Integer value) {
        for (Map.Entry<String, Integer> entry : this.values.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }

        return Integer.toString(value);
    }

    @Override
    public Integer parse(String value) {
        Integer result = this.values.get(value.toLowerCase(Locale.ROOT));
        if (result == null) {
            throw new IllegalArgumentException("Invalid value for gamerule " + name + ": " + value);
        }

        return result;
    }

    @Override
    public CommandArgumentType<Integer> argumentType() {
        return CommandArgumentTypes.fixedEnumMapped(this.name + "Values", this::parse,
                this.values.keySet().toArray(String[]::new));
    }

    @Override
    public int hashCode() {
        return Objects.hash(EnumGameRule.class, this.name.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof EnumGameRule<?> that)) {
            return false;
        }

        return this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public String toString() {
        return "EnumGameRule(name=" + this.name + ")";
    }
}
