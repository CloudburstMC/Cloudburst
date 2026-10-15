package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.data.serializer.MapSerializer;
import org.cloudburstmc.server.item.data.serializer.PrimitiveSerializer;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MapItemSerializerTest {

    @Test
    void keepsScaleInMapDataRatherThanCopyingItOntoTheItem() {
        MapView map = InterfaceProxy.create(MapView.class, Map.of("getScale", MapScale.NORMAL, "isTrackingPosition", true));
        FilledMapItemSerializer serializer = new FilledMapItemSerializer(id -> Optional.of(map));
        ItemStack item = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, 42L).build();
        NbtMapBuilder tag = NbtMap.builder();

        serializer.serialize(item, tag);
        NbtMap serialized = tag.build();

        assertEquals(42L, serialized.getLong("map_uuid"));
        assertEquals(1, serialized.getByte("map_display_players"));
        assertFalse(serialized.containsKey("map_scale"));
    }

    @Test
    void preservesTheMapReferenceWithoutEmbeddingRenderedPixels() {
        long id = 1L << 40;
        ItemStack item = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, id).build();
        NbtMapBuilder tag = NbtMap.builder();
        MapSerializer serializer = new MapSerializer();

        serializer.serialize(item, tag, id);
        NbtMap serialized = tag.build();

        assertEquals(id, serializer.deserialize(ItemTypes.FILLED_MAP.getId(), serialized));
        assertFalse(serialized.containsKey("map_name_index"));
        assertFalse(serialized.containsKey("MapData"));
        assertNull(serializer.deserialize(ItemTypes.FILLED_MAP.getId(), NbtMap.EMPTY));
    }

    @Test
    void preservesOrdinaryAndLocatorEmptyMapVariants() {
        EmptyMapItemSerializer serializer = new EmptyMapItemSerializer();

        for (short variant : new short[]{0, 2}) {
            ItemStackBuilder builder = ItemStack.builder(ItemTypes.EMPTY_MAP);
            serializer.deserialize(ItemTypes.EMPTY_MAP.getId(), variant, builder, NbtMap.EMPTY);
            ItemStack item = builder.build();

            assertEquals(variant, serializer.getAuxValue(item));
            assertEquals(variant == 2, item.getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false));
        }
    }

    @Test
    void filledMapTrackingMatchesItsItemData() {
        MapSerializer serializer = new MapSerializer();
        PrimitiveSerializer<Boolean> trackingSerializer = new PrimitiveSerializer<>("map_display_players", Boolean.class);

        for (boolean tracking : new boolean[]{false, true}) {
            ItemStack item = ItemStack.builder(ItemTypes.FILLED_MAP)
                    .setData(ItemDataComponents.MAP_ID, 42L)
                    .setData(ItemDataComponents.MAP_TRACKING_POSITION, tracking)
                    .build();
            NbtMapBuilder tag = NbtMap.builder();
            serializer.serialize(item, tag, 42L);

            assertEquals(42, tag.build().getInt("map_name_index"));
            assertEquals(tracking, trackingSerializer.deserialize(ItemTypes.FILLED_MAP.getId(), tag.build()));
        }
    }
}
