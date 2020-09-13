package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemDataSerializer<T> {

    String ITEM_TAG = "tag";
    String NAME_TAG = "tag";

    void serialize(ItemStack item, NbtMapBuilder itemTag, T value);

    T deserialize(Identifier id, Integer meta, NbtMap tag);
}
