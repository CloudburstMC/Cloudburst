package org.cloudburstmc.api.item;

import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A registered kind of item.
 */
public class ItemType {

    private final Identifier id;

    private ItemType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates an item type.
     *
     * @param id the item identifier
     * @return the item type
     */
    public static ItemType of(Identifier id) {
        return new ItemType(id);
    }

    /**
     * Returns the item identifier.
     *
     * @return the item identifier
     */
    public Identifier getId() {
        return id;
    }

    /**
     * Returns whether stacks of this type are empty.
     *
     * @return {@code true} for {@code minecraft:air}
     */
    public boolean isAir() {
        return "minecraft".equals(id.getNamespace()) && "air".equals(id.getName());
    }

    @Override
    public String toString() {
        return "ItemType{id=" + id + ')';
    }
}
