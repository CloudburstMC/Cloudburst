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

    public static ItemType of(Identifier id) {
        return of(id, new DataKey[0]);
    }

    public static ItemType of(Identifier id, DataKey<?, ?>... dataKeys) {
        checkNotNull(id, "id");
        checkNotNull(dataKeys, "dataKeys");

        return new ItemType(id, Set.of(dataKeys));
    }

    public Identifier getId() {
        return id;
    }

    public Set<DataKey<?, ?>> getDataKeys() {
        return dataKeys;
    }

    @Override
    public String toString() {
        return "ItemType{id=" + id + ')';
    }
}
