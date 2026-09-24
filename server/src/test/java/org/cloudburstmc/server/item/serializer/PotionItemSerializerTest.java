package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PotionItemSerializerTest {

    @Test
    public void keepsPotionVariantSeparateFromDurability() {
        PotionItemSerializer serializer = new PotionItemSerializer();
        for (ItemType type : List.of(ItemTypes.POTION, ItemTypes.SPLASH_POTION, ItemTypes.LINGERING_POTION)) {
            for (short id = 0; id < PotionTypes.values().size(); id++) {
                ItemStackBuilder builder = ItemStack.builder(type);
                serializer.deserialize(type.getId(), id, builder, NbtMap.EMPTY);

                ItemStack potion = builder.build();
                assertEquals(id, serializer.getAuxValue(potion));
                assertNull(potion.get(ItemDataComponents.DAMAGE));
            }
        }
    }
}
