package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

public interface ItemDataSerializer<T> {

    String ITEM_TAG = "tag";
    String NAME_TAG = "tag";

    void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, T value);

    T deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag);
}
