package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.data.serializer.BannerDataSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BannerSerializerTest {

    @Test
    void auxiliaryColorUsesDyeOrderAndDoesNotRequireADuplicateColorComponent() {
        BannerSerializer serializer = new BannerSerializer();
        for (DyeColor color : List.of(DyeColor.WHITE, DyeColor.RED, DyeColor.BLACK)) {
            ItemStack item = ItemStack.builder(ItemTypes.BANNER).setData(ItemDataComponents.BANNER_DATA, BannerData.of(0, color, List.of())).build();

            assertEquals(color.getDyeData(), serializer.getAuxValue(item));

            ItemStackBuilder restored = ItemStack.builder(ItemTypes.BANNER);
            serializer.deserialize(ItemTypes.BANNER.getId(), (short) color.getDyeData(), restored, NbtMap.EMPTY);
            assertSame(color, restored.build().get(ItemDataComponents.COLOR));
        }
    }

    @Test
    void washedPatternDataHasValueEqualityAfterSerialization() {
        BannerData data = BannerData.of(0, DyeColor.WHITE,
                List.of(new BannerPattern(BannerPattern.Type.PATTERN_BOTTOM_STRIPE, DyeColor.RED)));
        ItemStack item = ItemStack.builder(ItemTypes.BANNER).setData(ItemDataComponents.BANNER_DATA, data).build();
        BannerDataSerializer serializer = new BannerDataSerializer();
        NbtMapBuilder tag = NbtMap.builder();

        serializer.serialize(item, tag, data);
        BannerData restored = serializer.deserialize(ItemTypes.BANNER.getId(), tag.build());

        assertNotNull(restored);
        assertEquals(data, restored);
        assertEquals(data.hashCode(), restored.hashCode());
    }
}
