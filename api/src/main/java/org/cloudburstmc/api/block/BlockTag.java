package org.cloudburstmc.api.block;

import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link Identifier}-keyed tag that a {@link BlockType} may belong to.
 */
public final class BlockTag {

    private final Identifier id;

    private BlockTag(Identifier id) {
        this.id = id;
    }

    public static BlockTag of(Identifier id) {
        checkNotNull(id, "id");
        return new BlockTag(id);
    }

    public static BlockTag of(String id) {
        return of(Identifier.parse(id));
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockTag other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
