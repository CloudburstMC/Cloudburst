package org.cloudburstmc.server.container.grindstone;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class GrindstoneResultCalculatorTest {

    @BeforeAll
    static void initializeBlockTypes() {
        Objects.requireNonNull(BlockTestRegistry.get());
    }

    @Test
    void repairsMatchingArmorAndPreservesTheTopItemsData() {
        Enchantment curse = new Enchantment(EnchantmentTypes.BINDING, 1);
        ArmorTrim trim = new ArmorTrim("bolt", "resin");
        ItemStack top = ItemStack.from(ItemTypes.DIAMOND_CHESTPLATE).withDamage(400).toBuilder()
                .setData(ItemDataComponents.CUSTOM_NAME, "Keepsake")
                .setData(ItemDataComponents.ARMOR_TRIM, trim)
                .setData(ItemDataComponents.REPAIR_COST, 31)
                .build();
        ItemStack bottom = ItemStack.from(ItemTypes.DIAMOND_CHESTPLATE).withDamage(450).toBuilder()
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(curse.type(), curse))
                .build();

        ItemStack result = GrindstoneResultCalculator.calculate(top, bottom);
        int durability = CloudItemRegistry.get().requireComponent(top.getType(), ItemBehaviors.GET_MAX_DAMAGE).execute(top);

        assertSame(top.getType(), result.getType());
        assertEquals(Math.max(0, 850 - durability - durability / 20), result.getDamage());
        assertEquals("Keepsake", result.get(ItemDataComponents.CUSTOM_NAME));
        assertEquals(trim, result.get(ItemDataComponents.ARMOR_TRIM));
        assertEquals(Map.of(curse.type(), curse), result.get(ItemDataComponents.ENCHANTMENTS));
        assertEquals(Integer.valueOf(1), result.get(ItemDataComponents.REPAIR_COST));
    }

    @Test
    void disenchantsABookAndCountsOnlyRemovedEnchantmentsForExperience() {
        Enchantment sharpness = new Enchantment(EnchantmentTypes.SHARPNESS, 5);
        Enchantment curse = new Enchantment(EnchantmentTypes.VANISHING, 1);
        ItemStack book = ItemStack.from(ItemTypes.ENCHANTED_BOOK).toBuilder()
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(sharpness.type(), sharpness))
                .build();
        ItemStack cursed = book.toBuilder()
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(sharpness.type(), sharpness, curse.type(), curse))
                .build();

        ItemStack clean = GrindstoneResultCalculator.calculate(ItemStack.EMPTY, book);
        ItemStack stillCursed = GrindstoneResultCalculator.calculate(cursed, ItemStack.EMPTY);

        assertSame(ItemTypes.BOOK, clean.getType());
        assertFalse(clean.has(ItemDataComponents.ENCHANTMENTS));
        assertSame(ItemTypes.ENCHANTED_BOOK, stillCursed.getType());
        assertEquals(Map.of(curse.type(), curse), stillCursed.get(ItemDataComponents.ENCHANTMENTS));
        assertEquals(90, GrindstoneResultCalculator.experienceCost(book, cursed));
    }

    @Test
    void clearsPriorWorkWithoutRepairingASingleItem() {
        ItemStack sword = ItemStack.from(ItemTypes.IRON_SWORD).withDamage(32).toBuilder()
                .setData(ItemDataComponents.REPAIR_COST, 7)
                .build();

        ItemStack result = GrindstoneResultCalculator.calculate(sword, ItemStack.EMPTY);

        assertEquals(32, result.getDamage());
        assertEquals(Integer.valueOf(0), result.get(ItemDataComponents.REPAIR_COST));
        assertEquals(0, GrindstoneResultCalculator.experienceCost(sword, ItemStack.EMPTY));
        assertSame(ItemStack.EMPTY, GrindstoneResultCalculator.calculate(sword, ItemStack.from(ItemTypes.DIAMOND_SWORD)));
    }

    @Test
    void acceptsAnUntaggedSwordOnlyOnceWithoutAwardingExperience() {
        ItemStack sword = ItemStack.from(ItemTypes.DIAMOND_SWORD);

        ItemStack result = GrindstoneResultCalculator.calculate(sword, ItemStack.EMPTY);

        assertSame(sword.getType(), result.getType());
        assertEquals(Integer.valueOf(0), result.get(ItemDataComponents.REPAIR_COST));
        assertEquals(0, GrindstoneResultCalculator.experienceCost(sword, ItemStack.EMPTY));
        assertSame(ItemStack.EMPTY, GrindstoneResultCalculator.calculate(result, ItemStack.EMPTY));
    }
}
