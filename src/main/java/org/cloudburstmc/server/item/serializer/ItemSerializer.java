package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemSerializer {

    String ITEM_TAG = "tag";
    String NAME_TAG = "tag";

    void serialize(CloudItemStack item, NbtMapBuilder itemTag);

    CloudItemStack deserialize(Identifier id, Integer meta, NbtMap tag);
}
