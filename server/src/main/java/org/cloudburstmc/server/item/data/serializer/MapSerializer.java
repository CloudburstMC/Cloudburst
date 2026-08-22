package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.MapItem;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

/**
 * Serializes the persistent map identity and color payload stored on filled map items.
 */
public class MapSerializer implements ItemDataSerializer<MapItem> {

    private static final String TAG_ID = "mapId";
    private static final String TAG_PARENT_ID = "parentMapId";
    private static final String TAG_COLORS = "colors";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, MapItem value) {
        tag.putLong(TAG_ID, value.getId());
        tag.putLong(TAG_PARENT_ID, value.getParentId());
        tag.putByteArray(TAG_COLORS, value.getColors());
    }

    @Override
    public MapItem deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey(TAG_ID)) {
            return null;
        }

        return MapItem.of(
                tag.getLong(TAG_ID),
                tag.getLong(TAG_PARENT_ID),
                tag.getByteArray(TAG_COLORS)
        );
    }
}
