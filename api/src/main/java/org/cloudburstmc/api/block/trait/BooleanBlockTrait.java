package org.cloudburstmc.api.block.trait;

import org.cloudburstmc.api.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A trait accepting {@code false} and {@code true}.
 */
public final class BooleanBlockTrait extends BlockTrait<Boolean> {

    private BooleanBlockTrait(String name, boolean defaultValue) {
        super(Identifier.parse(name), Boolean.class, List.of(false, true), defaultValue);
    }

    /**
     * @param name the trait identifier
     * @return a trait whose default is false
     */
    public static BooleanBlockTrait of(String name) {
        return of(name, false);
    }

    /**
     * @param name         the trait identifier
     * @param defaultValue the default value
     * @return the trait
     */
    public static BooleanBlockTrait of(String name, boolean defaultValue) {
        return new BooleanBlockTrait(name, defaultValue);
    }

    /**
     * Accepts {@code true} and {@code false}, ignoring case.
     */
    @Override
    public Optional<Boolean> parseValue(String text) {
        Objects.requireNonNull(text, "text");
        if (text.equalsIgnoreCase("true")) {
            return Optional.of(true);
        }
        return text.equalsIgnoreCase("false") ? Optional.of(false) : Optional.empty();
    }
}
