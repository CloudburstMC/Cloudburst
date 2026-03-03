package org.cloudburstmc.api.item;

import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ItemType {

    private final Identifier id;
    private final Set<DataKey<?, ?>> dataKeys;

    public ItemType(Identifier id, Set<DataKey<?, ?>> dataKeys) {
        this.id = id;
        this.dataKeys = dataKeys;
    }

    public final Identifier getId() {
        return id;
    }

    public Set<DataKey<?, ?>> getDataKeys() {
        return dataKeys;
    }

    @Override
    public String toString() {
        return "ItemType{id=" + id + ')';
    }

    public static ItemType of(Identifier id) {
        return of(id, new DataKey[0]);
    }

    public static ItemType of(Identifier id, DataKey<?, ?>... dataKeys) {
        checkNotNull(id, "id");

        return new ItemType(id, Set.of(dataKeys));
    }
}
