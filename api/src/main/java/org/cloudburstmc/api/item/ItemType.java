package org.cloudburstmc.api.item;

import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A registered kind of item.
 */
public final class ItemType {

    private final Identifier id;
    private final Set<DataKey<?, ?>> dataKeys;

    private ItemType(Identifier id, Set<DataKey<?, ?>> dataKeys) {
        this.id = checkNotNull(id, "id");
        this.dataKeys = Set.copyOf(checkNotNull(dataKeys, "dataKeys"));
    }

    /**
     * Creates an item type without supported metadata keys.
     *
     * @param id the item identifier
     * @return the item type
     */
    public static ItemType of(Identifier id) {
        return of(id, new DataKey[0]);
    }

    /**
     * Creates an item type with its supported metadata keys.
     *
     * @param id       the item identifier
     * @param dataKeys the metadata keys supported by the item
     * @return the item type
     */
    public static ItemType of(Identifier id, DataKey<?, ?>... dataKeys) {
        checkNotNull(id, "id");
        checkNotNull(dataKeys, "dataKeys");

        return new ItemType(id, Set.of(dataKeys));
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

    /**
     * Returns the metadata keys supported by this item type.
     *
     * @return an immutable set of supported metadata keys
     */
    public Set<DataKey<?, ?>> getDataKeys() {
        return dataKeys;
    }

    @Override
    public String toString() {
        return "ItemType{id=" + id + ')';
    }
}
