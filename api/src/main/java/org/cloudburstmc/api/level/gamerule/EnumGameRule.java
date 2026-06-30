package org.cloudburstmc.api.level.gamerule;

import lombok.Getter;

import java.util.*;

public final class EnumGameRule<E extends Enum<E>> implements GameRule<Integer> {

    private static final Class<Integer> CLASS = Integer.class;

    private final String name;
    @Getter
    private final Class<E> enumClass;
    private final Integer defaultValue;
    private final Map<String, Integer> values;

    private EnumGameRule(String name, Class<E> enumClass, int defaultValue, Map<String, Integer> values) {
        this.name = name;
        this.enumClass = enumClass;
        this.defaultValue = defaultValue;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static <E extends Enum<E>> EnumGameRule<E> of(String name, Class<E> enumClass, E defaultValue) {
        return of(name, enumClass, defaultValue, enumClass.getEnumConstants());
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumGameRule<E> of(String name, Class<E> enumClass, E defaultValue, E... allowedValues) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (E value : allowedValues) {
            values.put(value.name().toLowerCase(Locale.ROOT), value.ordinal());
        }
        return new EnumGameRule<>(name, enumClass, defaultValue.ordinal(), values);
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

    public Set<String> getValues() {
        return this.values.keySet();
    }

    public String getSerializedValue(int value) {
        for (Map.Entry<String, Integer> entry : this.values.entrySet()) {
            if (entry.getValue() == value) {
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
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "EnumGameRule(name=" + name + ")";
    }
}
