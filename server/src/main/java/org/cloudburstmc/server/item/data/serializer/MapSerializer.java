package org.cloudburstmc.server.item.data.serializer;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.MapItem;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class MapSerializer implements ItemDataSerializer<MapItem> {

    private static final String TAG_ID = "mapId"; // Long
    private static final String TAG_PARENT_ID = "parentMapId"; // Long
    private static final String TAG_DIMENSION = "dimension"; // Byte
    private static final String TAG_X_CENTER = "xCenter"; // Int
    private static final String TAG_Z_CENTER = "zCenter"; // Int
    private static final String TAG_SCALE = "scale"; // Byte
    private static final String TAG_UNLIMITED_TRACKING = "unlimitedTracking"; // Boolean
    private static final String TAG_PREVIEW_INCOMPLETE = "previewIncomplete"; // Boolean
    private static final String TAG_WIDTH = "width"; // Short
    private static final String TAG_HEIGHT = "height"; // Short
    private static final String TAG_COLORS = "colors"; // Byte array
    private static final String TAG_FULLY_EXPLORED = "fullyExplored"; // Boolean
    private static final String TAG_DECORATIONS = "decorations"; // List<NbtMap>
    private static final String TAG_DECORATION_DATA = "data"; // NbtMap - MapDecoration::load
    private static final String TAG_DECORATION_KEY = "key"; // NbtMap - MapItemTrackedActor::UniqueId::load
    private static final String TAG_LOCKED = "mapLocked"; // Boolean

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
