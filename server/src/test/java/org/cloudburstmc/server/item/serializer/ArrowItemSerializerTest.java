package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.network.NetworkUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrowItemSerializerTest {

    @Test
    void preservesEveryPotionVariantThroughInventoryAndPersistence() {
        ItemData ordinaryArrow = ItemStackPacketCodec.toNetwork(ItemStack.from(ItemTypes.ARROW));
        for (PotionType potion : PotionTypes.values()) {
            int auxiliaryValue = NetworkUtils.potionToNetwork(potion) + 1;
            ItemData creativeArrow = ordinaryArrow.toBuilder().damage(auxiliaryValue).count(12).build();

            ItemStack picked = ItemStackPacketCodec.fromNetwork(creativeArrow);

            assertEquals(potion, picked.get(ItemDataComponents.POTION_TYPE));
            assertNull(picked.get(ItemDataComponents.DAMAGE));
            assertEquals(12, picked.getCount());
            assertEquals(auxiliaryValue, ItemStackPacketCodec.toNetwork(picked).getDamage());

            ItemStack restored = ItemStackNbtCodec.deserializeItem(ItemStackNbtCodec.serializeItem(picked));
            assertEquals(picked, restored);
            assertEquals(auxiliaryValue, ItemStackPacketCodec.toNetwork(restored).getDamage());
            assertTrue(picked.isStackableWith(restored));
            assertFalse(picked.isStackableWith(ItemStack.from(ItemTypes.ARROW)));
        }
    }

    @Test
    void keepsOrdinaryArrowsDistinctFromWaterTippedArrows() {
        ItemStack ordinary = ItemStack.from(ItemTypes.ARROW);
        ItemStack water = ordinary.toBuilder().setData(ItemDataComponents.POTION_TYPE, PotionTypes.WATER).build();

        ItemStack restored = ItemStackPacketCodec.fromNetwork(ItemStackPacketCodec.toNetwork(ordinary));

        assertEquals(ordinary, restored);
        assertFalse(restored.has(ItemDataComponents.POTION_TYPE));
        assertEquals(0, ItemStackPacketCodec.toNetwork(restored).getDamage());
        assertEquals(1, ItemStackPacketCodec.toNetwork(water).getDamage());
        assertFalse(restored.isStackableWith(water));
    }
}
