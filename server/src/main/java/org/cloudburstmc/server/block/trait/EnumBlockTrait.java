package org.cloudburstmc.server.block.trait;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class EnumBlockTrait<E extends Enum<E>> extends BlockTrait<E> {

    private final E defaultValue;

    private EnumBlockTrait(String name, @Nullable String vanillaName, Class<E> enumClass, ImmutableList<E> possibleValues, E defaultValue) {
        super(name, vanillaName, enumClass, possibleValues, false);
        this.defaultValue = defaultValue;
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass) {
        return of(name, null, enumClass);
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, @Nullable String vanillaName, Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        return of(name, vanillaName, enumClass, ImmutableSet.copyOf(values), values[0]);
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, E... possibleValues) {
        return of(name, null, enumClass, possibleValues);
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, @Nullable String vanillaName, Class<E> enumClass, E... possibleValues) {
        return of(name, vanillaName, enumClass, ImmutableSet.copyOf(possibleValues), possibleValues[0]);
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, E defaultValue) {
        return of(name, enumClass, ImmutableSet.copyOf(enumClass.getEnumConstants()), defaultValue);
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, Set<E> possibleValues, E defaultValue) {
        return of(name, null, enumClass, possibleValues, defaultValue);
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, @Nullable String vanillaName, Class<E> enumClass, Set<E> possibleValues, E defaultValue) {
        checkNotNull(name, "name");
        checkNotNull(enumClass, "enumClass");
        checkNotNull(possibleValues, "possibleValues");
        checkNotNull(defaultValue, "defaultValues");
        return new EnumBlockTrait<>(name, vanillaName, enumClass, ImmutableList.copyOf(possibleValues), defaultValue);
    }

    @Override
    public E getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public int getIndex(Object value) {
        checkNotNull(value, "value");
        int index = this.possibleValues.indexOf(value);
        checkArgument(index != -1, "Invalid block trait");
        return index;
    }

    @Override
    public E parseValue(String text) {
        return this.getPossibleValues().stream().filter(e -> e.name().equalsIgnoreCase(text)).findAny().orElse(this.defaultValue);
    }

    @Override
    public E parseStorageValue(Object value) {
        if (value instanceof String) {
            return parseValue((String) value);
        }

        return this.valueClass.getEnumConstants()[(int) value];
    }
}
