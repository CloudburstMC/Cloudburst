package org.cloudburstmc.api.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;

public interface ItemSerializer {
    String ITEM_TAG = "tag";
    String NAME_TAG = "Name";

    void serialize(ItemStack item, NbtMapBuilder itemTag);

    void deserialize(Identifier id, short meta, int amount, ItemStackBuilder builder, NbtMap tag);
}
