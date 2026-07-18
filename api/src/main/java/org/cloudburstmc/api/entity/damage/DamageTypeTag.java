package org.cloudburstmc.api.entity.damage;

import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.util.Identifier;

import static java.util.Objects.requireNonNull;

/**
 * Identifies a group of damage types that share behavior.
 */
@EqualsAndHashCode(of = "id")
public final class DamageTypeTag {

    private final Identifier id;

    DamageTypeTag(Identifier id) {
        this.id = requireNonNull(id, "id");
    }

    /**
     * Returns the tag identifier.
     *
     * @return the tag identifier
     */
    public Identifier getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "#" + this.id;
    }
}
