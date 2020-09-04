package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemDataSerializer<T> {

    void serialize(ItemStack item, NbtMapBuilder tag, T value);

    T deserialize(Identifier type, Integer meta, NbtMap tag);
}
