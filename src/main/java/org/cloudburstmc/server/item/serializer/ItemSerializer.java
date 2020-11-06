package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemSerializer {

    String ITEM_TAG = "tag";
    String NAME_TAG = "Name";

    void serialize(CloudItemStack item, NbtMapBuilder itemTag);

    void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag);
}
