package org.cloudburstmc.api.block.trait;

import org.cloudburstmc.api.util.Identifier;

import java.util.*;

/**
 * A trait accepting a nonempty selection of constants from one enum.
 *
 * @param <E> the enum type
 */
public final class EnumBlockTrait<E extends Enum<E>> extends BlockTrait<E> {

    private final Map<String, E> valuesByName;

    private EnumBlockTrait(String name, Class<E> enumClass, List<E> values, E defaultValue) {
        super(Identifier.parse(name), requireEnum(enumClass), values, defaultValue);
        Map<String, E> names = new HashMap<>();
        for (E value : this.getPossibleValues()) {
            if (names.putIfAbsent(value.name().toLowerCase(Locale.ROOT), value) != null) {
                throw new IllegalArgumentException("Enum trait names must be distinct ignoring case: " + name);
            }
        }
        this.valuesByName = Map.copyOf(names);
    }

    /**
     * @param name      the trait identifier
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return a trait accepting all constants, defaulting to the first
     * @throws IllegalArgumentException if the enum has no constants
     */
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass) {
        E[] values = enumValues(enumClass);
        return new EnumBlockTrait<>(name, enumClass, Arrays.asList(values), values[0]);
    }

    /**
     * @param name           the trait identifier
     * @param enumClass      the enum class
     * @param possibleValues the distinct constants to accept
     * @param <E>            the enum type
     * @return a trait defaulting to the first accepted constant
     * @throws IllegalArgumentException if the selection is empty or contains duplicates
     */
    @SafeVarargs
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, E... possibleValues) {
        Objects.requireNonNull(possibleValues, "possibleValues");
        if (possibleValues.length == 0) {
            throw new IllegalArgumentException("Enum trait values must not be empty");
        }
        return new EnumBlockTrait<>(name, enumClass, Arrays.asList(possibleValues.clone()), possibleValues[0]);
    }

    /**
     * @param name         the trait identifier
     * @param enumClass    the enum class
     * @param defaultValue the default constant
     * @param <E>          the enum type
     * @return a trait accepting all constants
     */
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, E defaultValue) {
        return new EnumBlockTrait<>(name, enumClass, Arrays.asList(enumValues(enumClass)), defaultValue);
    }

    /**
     * @param name           the trait identifier
     * @param enumClass      the enum class
     * @param possibleValues the accepted constants
     * @param defaultValue   the default constant within the selection
     * @param <E>            the enum type
     * @return the trait
     * @throws IllegalArgumentException if the selection or default is invalid
     */
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, Set<E> possibleValues, E defaultValue) {
        return new EnumBlockTrait<>(name, enumClass, List.copyOf(possibleValues), defaultValue);
    }

    /**
     * Parses an accepted enum constant's name, ignoring case.
     */
    @Override
    public Optional<E> parseValue(String text) {
        String name = Objects.requireNonNull(text, "text").toLowerCase(Locale.ROOT);
        return Optional.ofNullable(this.valuesByName.get(name));
    }

    private static <E extends Enum<E>> E[] enumValues(Class<E> enumClass) {
        E[] values = requireEnum(enumClass).getEnumConstants();
        if (values.length == 0) {
            throw new IllegalArgumentException("Trait requires a nonempty enum class");
        }
        return values;
    }

    private static <E extends Enum<E>> Class<E> requireEnum(Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass");
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Trait value class must be an enum");
        }
        return enumClass;
    }
}
