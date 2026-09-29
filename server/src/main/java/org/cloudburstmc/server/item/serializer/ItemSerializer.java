package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public interface ItemSerializer {

    String ITEM_TAG = "tag";
    String NAME_TAG = "Name";

    int getAuxValue(ItemStack item);

    void serialize(ItemStack item, NbtMapBuilder tag);

    void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag);
}
