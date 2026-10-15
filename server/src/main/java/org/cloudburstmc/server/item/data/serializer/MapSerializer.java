package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

/**
 * Serializes the reference to separately stored map data.
 */
public class MapSerializer implements ItemDataComponentSerializer<Long> {

    private static final String TAG_ID = "map_uuid";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, Long value) {
        tag.putLong(TAG_ID, value);
        if (value >= 0 && value <= Integer.MAX_VALUE) {
            tag.putInt("map_name_index", value.intValue());
        }
        tag.putByte("map_display_players", (byte) (item.getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false) ? 1 : 0));
    }

    @Override
    public Long deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey(TAG_ID)) {
            return null;
        }

        return tag.getLong(TAG_ID);
    }
}
