package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collections;
import java.util.Map;

public interface ItemSerializer {

    String ITEM_TAG = "tag";
    String NAME_TAG = "Name";

    default Map<Class<?>, Object> getDefaultMetadataValues() {
        return Collections.emptyMap();
    }

    void serialize(ItemStack item, NbtMapBuilder tag);

    void deserialize(ItemStackBuilder builder, NbtMap tag);
}
