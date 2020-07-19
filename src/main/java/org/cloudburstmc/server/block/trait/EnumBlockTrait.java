package org.cloudburstmc.server.block.trait;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class EnumBlockTrait<E extends Enum<E>> implements BlockTrait<E> {
    private final String name;
    private final Class<E> enumClass;
    private final ImmutableList<E> possibleValues;
    private final E defaultValue;

    private EnumBlockTrait(String name, Class<E> enumClass, ImmutableList<E> possibleValues, E defaultValue) {
        this.name = name;
        this.enumClass = enumClass;
        this.possibleValues = possibleValues;
        this.defaultValue = defaultValue;
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        return of(name, enumClass, values);
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, E... possibleValues) {
        return of(name, enumClass, ImmutableSet.copyOf(possibleValues), possibleValues[0]);
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, E defaultValue) {
        return of(name, enumClass, ImmutableSet.copyOf(enumClass.getEnumConstants()), defaultValue);
    }

    public static <E extends Enum<E>> EnumBlockTrait<E> of(String name, Class<E> enumClass, Set<E> possibleValues,
                                                           E defaultValue) {
        checkNotNull(name, "name");
        checkNotNull(enumClass, "enumClass");
        checkNotNull(possibleValues, "possibleValues");
        checkNotNull(defaultValue, "defaultValues");
        return new EnumBlockTrait<>(name, enumClass, ImmutableList.copyOf(new HashSet<>(possibleValues)), defaultValue);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<E> getPossibleValues() {
        return this.possibleValues;
    }

    @Override
    public E getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Class<E> getValueClass() {
        return this.enumClass;
    }

    public int getIndex(Object value) {
        checkNotNull(value, "value");
        int index = this.possibleValues.indexOf(value);
        checkArgument(index != -1, "Invalid block trait");
        return index;
    }
}
