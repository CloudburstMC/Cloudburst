package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.MapItem;
import org.cloudburstmc.server.utils.Identifier;

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
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, MapItem value) {
        dataTag.putLong(TAG_ID, value.getId());
        dataTag.putLong(TAG_PARENT_ID, value.getParentId());
        dataTag.putByteArray(TAG_COLORS, value.getColors());
    }

    @Override
    public MapItem deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        if (!dataTag.containsKey(TAG_ID)) {
            return null;
        }

        return MapItem.of(
                dataTag.getLong(TAG_ID),
                dataTag.getLong(TAG_PARENT_ID),
                dataTag.getByteArray(TAG_COLORS)
        );
    }
}
