package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.item.*;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.CloudRecipeRegistry.SmithingResult;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SmithingRecipeTest {

    @BeforeAll
    static void initializeBlockTypes() {
        Objects.requireNonNull(BlockTestRegistry.get());
    }

    @Test
    void upgradesDiamondArmorWithoutResettingItsData() {
        ItemStack template = ItemStack.from(ItemTypes.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack base = ItemStack.from(ItemTypes.DIAMOND_CHESTPLATE).withDamage(73).toBuilder()
                .setData(ItemDataComponents.CUSTOM_NAME, "My armor")
                .setData(ItemDataComponents.ARMOR_TRIM, new ArmorTrim("sentry", "gold"))
                .build();
        ItemStack addition = ItemStack.from(ItemTypes.NETHERITE_INGOT);

        SmithingResult result = Objects.requireNonNull(CloudRecipeRegistry.get().findSmithingResult(template, base, addition));

        assertSame(ItemTypes.NETHERITE_CHESTPLATE, result.item().getType());
        assertEquals(base.getDataComponents(), result.item().getDataComponents());
        assertEquals(1, result.templateCost());
        assertEquals(1, result.baseCost());
        assertEquals(1, result.additionCost());
        assertNull(CloudRecipeRegistry.get().findSmithingResult(template, base, ItemStack.from(ItemTypes.GOLD_INGOT)));
    }

    @Test
    void trimsArmorButRejectsTheSameTrimAndNonArmor() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        ItemStack template = ItemStack.from(ItemTypes.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        ItemStack base = ItemStack.from(ItemTypes.TURTLE_HELMET).withDamage(9);
        ItemStack addition = ItemStack.from(ItemTypes.COPPER_INGOT);

        SmithingResult result = Objects.requireNonNull(recipes.findSmithingResult(template, base, addition));

        assertSame(base.getType(), result.item().getType());
        assertEquals(9, result.item().getDamage());
        assertEquals(new ArmorTrim("sentry", "copper"), result.item().get(ItemDataComponents.ARMOR_TRIM));
        assertNull(recipes.findSmithingResult(template, result.item(), addition));
        assertNull(recipes.findSmithingResult(template, ItemStack.from(ItemTypes.IRON_PICKAXE), addition));
    }

    @Test
    void roundTripsArmorTrimThroughTheItemSerializer() {
        ArmorTrim trim = new ArmorTrim("bolt", "resin");
        ItemStack item = ItemStack.from(ItemTypes.IRON_BOOTS).toBuilder()
                .setData(ItemDataComponents.ARMOR_TRIM, trim)
                .build();
        NbtMapBuilder builder = NbtMap.builder();
        CloudItemRegistry.get().getSerializer(item.getType()).serialize(item, builder);
        NbtMap tag = builder.build();

        assertEquals("bolt", tag.getCompound("Trim").getString("Pattern"));
        assertEquals("resin", tag.getCompound("Trim").getString("Material"));

        ItemStackBuilder restored = ItemStack.from(item.getType()).toBuilder();
        CloudItemRegistry.get().getSerializer(item.getType()).deserialize(item.getType().getId(), (short) 0, restored, tag);
        assertEquals(trim, restored.build().get(ItemDataComponents.ARMOR_TRIM));
    }
}
