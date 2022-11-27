package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Collections;
import java.util.Map;

public interface ItemSerializer {

    String ITEM_TAG = "tag";
    String NAME_TAG = "Name";

    default Map<Class<?>, Object> getDefaultMetadataValues() {
        return Collections.emptyMap();
    }

    void serialize(ItemStack item, NbtMapBuilder tag);

    void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag);
}
