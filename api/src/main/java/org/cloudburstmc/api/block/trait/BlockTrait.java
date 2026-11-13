package org.cloudburstmc.api.block.trait;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A typed property with a fixed, nonempty set of allowed block-state values.
 *
 * <p>Traits are identified by instance, not by name. Use the constants in
 * {@link org.cloudburstmc.api.block.BlockTraits} for built-in traits or obtain
 * a trait from {@link org.cloudburstmc.api.block.BlockType#getTraits()}.
 * Creating a trait with the same name does not make it interchangeable.
 *
 * @param <E> the value type
 */
public abstract sealed class BlockTrait<E extends Comparable<E>> permits BooleanBlockTrait, IntegerBlockTrait, EnumBlockTrait {

    private final Identifier name;
    private final Class<E> valueClass;
    private final List<E> possibleValues;
    private final E defaultValue;

    BlockTrait(Identifier name, Class<E> valueClass, List<E> possibleValues, E defaultValue) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass");
        this.possibleValues = List.copyOf(possibleValues);
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");

        if (this.possibleValues.isEmpty() || new HashSet<>(this.possibleValues).size() != this.possibleValues.size()) {
            throw new IllegalArgumentException("Trait values must be nonempty and distinct: " + name);
        }

        if (!this.possibleValues.stream().allMatch(valueClass::isInstance) || !this.possibleValues.contains(defaultValue)) {
            throw new IllegalArgumentException("Trait values and default must belong to the declared domain: " + name);
        }
    }

    /**
     * @return the trait identifier
     */
    public Identifier getName() {
        return this.name;
    }

    /**
     * @return the class of values accepted by this trait
     */
    public Class<E> getValueClass() {
        return this.valueClass;
    }

    /**
     * @return the immutable allowed values
     */
    public List<E> getPossibleValues() {
        return this.possibleValues;
    }

    /**
     * @return the value used in a block type's default state
     */
    public E getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * @param value the value to test
     * @return whether the value has the correct type and belongs to this trait's domain
     */
    public boolean testValue(@Nullable Object value) {
        return this.valueClass.isInstance(value) && this.possibleValues.contains(this.valueClass.cast(value));
    }

    /**
     * Parses a public trait value without substituting a default for invalid input.
     *
     * @param text the value text
     * @return the parsed value, or empty if the text is invalid for this trait
     */
    public abstract Optional<E> parseValue(String text);

    @Override
    public String toString() {
        return this.name.toString();
    }
}
