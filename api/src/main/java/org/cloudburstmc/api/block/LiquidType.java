package org.cloudburstmc.api.block;

import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;

import static java.util.Objects.requireNonNull;

/**
 * A kind of liquid, independent of its level and falling state.
 */
public final class LiquidType {

    /** The liquid identifier. */
    @Getter
    private final Identifier id;
    private final Identifier family;

    LiquidType(Identifier id, Identifier family) {
        this.id = requireNonNull(id, "id");
        this.family = requireNonNull(family, "family");
    }

    /**
     * @return whether this type represents the absence of liquid
     */
    public boolean isEmpty() {
        return this == LiquidTypes.EMPTY;
    }

    /**
     * Checks whether two liquid types share simulation rules.
     *
     * @param other the liquid type to compare
     * @return whether the types belong to the same family
     */
    public boolean isSameFamily(LiquidType other) {
        requireNonNull(other, "other");
        return this.family.equals(other.family);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
