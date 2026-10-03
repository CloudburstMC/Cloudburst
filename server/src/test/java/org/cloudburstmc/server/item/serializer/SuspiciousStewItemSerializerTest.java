package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuspiciousStewItemSerializerTest {

    private final SuspiciousStewItemSerializer serializer = new SuspiciousStewItemSerializer();

    @Test
    void preservesCreativeVariantsThroughSerialization() {
        for (short variant = 0; variant <= 12; variant++) {
            ItemStack stew = this.deserialize(variant, NbtMap.EMPTY);
            NbtMapBuilder tag = NbtMap.builder();
            this.serializer.serialize(stew, tag);
            ItemStack restored = this.deserialize((short) this.serializer.getAuxValue(stew), tag.build());
            assertEquals(stew.get(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS),
                    restored.get(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS));
        }

        assertEquals(List.of(new PotionEffect(EffectTypes.NAUSEA, 140, 0)),
                this.deserialize((short) 12, NbtMap.EMPTY).get(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS));
    }

    @Test
    void preservesEmptyAndCustomEffectsInsteadOfFallingBackToAnAuxiliaryVariant() {
        for (List<PotionEffect> effects : List.of(List.<PotionEffect>of(),
                List.of(new PotionEffect(EffectTypes.SPEED, 123, 2, true, false),
                        new PotionEffect(EffectTypes.REGENERATION, 90, 1)))) {
            ItemStack stew = ItemStack.from(ItemTypes.SUSPICIOUS_STEW).toBuilder()
                    .setData(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS, effects).build();
            NbtMapBuilder tag = NbtMap.builder();
            this.serializer.serialize(stew, tag);
            assertEquals(effects, this.deserialize((short) this.serializer.getAuxValue(stew), tag.build())
                    .get(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS));
        }

        NbtMapBuilder tag = NbtMap.builder();
        this.serializer.serialize(ItemStack.from(ItemTypes.SUSPICIOUS_STEW), tag);
        assertEquals(List.of(), this.deserialize((short) 0, tag.build()).get(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS));
        assertThrows(IllegalArgumentException.class, () -> this.deserialize((short) 13, NbtMap.EMPTY));
    }

    private ItemStack deserialize(short variant, NbtMap tag) {
        ItemStackBuilder builder = ItemStack.from(ItemTypes.SUSPICIOUS_STEW).toBuilder();
        this.serializer.deserialize(ItemTypes.SUSPICIOUS_STEW.getId(), variant, builder, tag);
        return builder.build();
    }
}
