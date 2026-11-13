package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BrewingRecipeTest {

    @BeforeAll
    static void initializeBlockTypes() {
        Objects.requireNonNull(BlockTestRegistry.get());
    }

    @Test
    void brewsAllBottleTypesFromStackedIngredients() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        ItemStack wart = ItemStack.from(ItemTypes.NETHER_WART, 12);

        for (ItemType type : List.of(ItemTypes.POTION, ItemTypes.SPLASH_POTION, ItemTypes.LINGERING_POTION)) {
            ItemStack water = ItemStack.from(type);
            ItemStack result = recipes.brewPotion(wart, water);

            assertSame(type, result.getType());
            assertEquals(PotionTypes.AWKWARD, result.get(ItemDataComponents.POTION_TYPE));
            assertEquals(1, result.getCount());
        }
    }

    @Test
    void preservesThePotionAndItsNameWhenChangingBottleType() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        ItemStack potion = ItemStack.from(ItemTypes.POTION).toBuilder()
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.STRONG_HEALING)
                .setData(ItemDataComponents.CUSTOM_NAME, "Healing brew")
                .build();

        ItemStack splash = recipes.brewPotion(ItemStack.from(ItemTypes.GUNPOWDER), potion);
        ItemStack lingering = recipes.brewPotion(ItemStack.from(ItemTypes.DRAGON_BREATH), splash);

        assertSame(ItemTypes.SPLASH_POTION, splash.getType());
        assertSame(ItemTypes.LINGERING_POTION, lingering.getType());
        assertEquals(potion.getDataComponents(), splash.getDataComponents());
        assertEquals(potion.getDataComponents(), lingering.getDataComponents());
    }

    @Test
    void leavesIncompatibleBottlesUnchanged() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        ItemStack bottle = ItemStack.from(ItemTypes.GLASS_BOTTLE);
        ItemStack water = ItemStack.from(ItemTypes.LINGERING_POTION);

        assertSame(bottle, recipes.brewPotion(ItemStack.from(ItemTypes.NETHER_WART), bottle));
        assertSame(water, recipes.brewPotion(ItemStack.from(ItemTypes.DIAMOND), water));
        assertSame(ItemStack.EMPTY, recipes.brewPotion(ItemStack.from(ItemTypes.NETHER_WART), ItemStack.EMPTY));
    }
}
