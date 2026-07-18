package org.cloudburstmc.api.entity.damage;

import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Describes the behavior shared by damage from the same source type.
 */
@EqualsAndHashCode(of = "id")
public final class DamageType {

    private final Identifier id;
    private final Set<DamageTypeTag> tags;

    DamageType(Identifier id, Set<DamageTypeTag> tags) {
        this.id = requireNonNull(id, "id");
        this.tags = Set.copyOf(tags);
    }

    /**
     * Returns the identifier of this damage type.
     *
     * @return the damage type identifier
     */
    public Identifier getId() {
        return this.id;
    }

    /**
     * Checks whether this damage type belongs to the supplied tag.
     *
     * @param tag the tag to test
     * @return whether this damage type belongs to the tag
     */
    public boolean is(DamageTypeTag tag) {
        return this.tags.contains(requireNonNull(tag, "tag"));
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
