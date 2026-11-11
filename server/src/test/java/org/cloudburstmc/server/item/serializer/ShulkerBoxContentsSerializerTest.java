package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.ShulkerBoxContents;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.data.serializer.ShulkerBoxContentsSerializer;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShulkerBoxContentsSerializerTest {

    @BeforeAll
    static void initializeRegistry() {
        BlockTestRegistry.ensureInitialized();
    }

    @Test
    void preservesOccupiedSlotsAndCustomName() {
        ShulkerBoxContents contents = new ShulkerBoxContents(Map.of(
                3, ItemStack.from(ItemTypes.STICK).withCount(7),
                26, ItemStack.from(ItemTypes.APPLE)));
        ItemStack box = ItemStack.builder(ItemTypes.BLUE_SHULKER_BOX)
                .setData(ItemDataComponents.CUSTOM_NAME, "Supplies")
                .setData(ItemDataComponents.SHULKER_BOX_CONTENTS, contents)
                .build();

        ItemStack restored = ItemStackNbtCodec.deserializeItem(ItemStackNbtCodec.serializeItem(box));

        assertEquals(ItemTypes.BLUE_SHULKER_BOX, restored.getType());
        assertEquals("Supplies", restored.get(ItemDataComponents.CUSTOM_NAME));
        assertEquals(contents, restored.get(ItemDataComponents.SHULKER_BOX_CONTENTS));
    }

    @Test
    void skipsNestedBoxesAndInvalidSlots() {
        NbtMap nested = ItemStackNbtCodec.serializeItem(ItemStack.from(ItemTypes.BLUE_SHULKER_BOX), 0);
        NbtMap outOfBounds = ItemStackNbtCodec.serializeItem(ItemStack.from(ItemTypes.STICK), 27);
        NbtMap tag = NbtMap.builder().putList("Items", NbtType.COMPOUND, List.of(nested, outOfBounds)).build();

        assertEquals(Map.of(), ShulkerBoxContentsSerializer.read(tag).items());
    }
}
