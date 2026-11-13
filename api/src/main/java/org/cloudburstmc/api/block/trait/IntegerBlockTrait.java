package org.cloudburstmc.api.block.trait;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.IntRange;

import java.util.Objects;
import java.util.Optional;

/**
 * A trait accepting integers within an inclusive, finite range.
 */
public final class IntegerBlockTrait extends BlockTrait<Integer> {

    private final IntRange range;

    private IntegerBlockTrait(String name, IntRange range, int defaultValue) {
        super(Identifier.parse(name), Integer.class, range, defaultValue);
        this.range = range;
    }

    /**
     * @param name   the trait identifier
     * @param length the positive number of values, starting at zero
     * @return a trait whose default is zero
     * @throws IllegalArgumentException if the length is not positive
     */
    public static IntegerBlockTrait of(String name, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Trait length must be positive");
        }
        return of(name, 0, length - 1);
    }

    /**
     * @param name  the trait identifier
     * @param start the inclusive minimum
     * @param end   the inclusive maximum
     * @return a trait whose default is the minimum
     * @throws IllegalArgumentException if the range is invalid
     */
    public static IntegerBlockTrait of(String name, int start, int end) {
        return of(name, start, end, start);
    }

    /**
     * @param name         the trait identifier
     * @param start        the inclusive minimum
     * @param end          the inclusive maximum
     * @param defaultValue the default value within the range
     * @return the trait
     * @throws IllegalArgumentException if the range or default is invalid
     */
    public static IntegerBlockTrait of(String name, int start, int end, int defaultValue) {
        if (defaultValue < start || defaultValue > end) {
            throw new IllegalArgumentException("Default value must belong to the trait range");
        }
        return new IntegerBlockTrait(name, new IntRange(start, end), defaultValue);
    }

    /**
     * @return the inclusive allowed range
     */
    public IntRange getRange() {
        return this.range;
    }

    @Override
    public boolean testValue(@Nullable Object value) {
        return value instanceof Integer number && number >= this.range.getStart() && number <= this.range.getEnd();
    }

    /**
     * Parses a decimal integer within the allowed range.
     */
    @Override
    public Optional<Integer> parseValue(String text) {
        Objects.requireNonNull(text, "text");
        try {
            int value = Integer.parseInt(text);
            return this.testValue(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
