package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public interface ItemDataSerializer<T> {

    String ITEM_TAG = "tag";
    String NAME_TAG = "tag";

    void serialize(ItemStack item, NbtMapBuilder tag, T value);

    T deserialize(Identifier id, NbtMap tag);
}
