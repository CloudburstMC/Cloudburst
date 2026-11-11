package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.server.crafting.CloudCookingRecipe;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CookingRecipeTest {

    @BeforeAll
    static void initializeBlockTypes() {
        Objects.requireNonNull(BlockTestRegistry.get());
    }

    @Test
    void keepsSharedRecipesSeparateByCookingStation() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        ItemStack chicken = ItemStack.from(ItemTypes.CHICKEN);

        CloudCookingRecipe furnace = Objects.requireNonNull(recipes.matchCookingRecipe(chicken, Identifiers.FURNACE));
        CloudCookingRecipe smoker = Objects.requireNonNull(recipes.matchCookingRecipe(chicken, Identifiers.LIT_SMOKER));

        assertNotEquals(furnace.getId(), smoker.getId());
        assertEquals(200, furnace.getCookingTime());
        assertEquals(100, smoker.getCookingTime());
        assertSame(ItemTypes.COOKED_CHICKEN, smoker.getResult().getType());
        assertNull(recipes.matchCookingRecipe(chicken, Identifiers.BLAST_FURNACE));
    }

    @Test
    void usesTheCharcoalInputTagAndTypedFuelValues() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        CloudItemRegistry items = CloudItemRegistry.get();
        ItemStack log = ItemStack.from(ItemTypes.OAK_LOG);
        ItemStack stem = ItemStack.from(ItemTypes.CRIMSON_STEM);

        CloudCookingRecipe recipe = Objects.requireNonNull(recipes.matchCookingRecipe(log, Identifiers.FURNACE));

        assertSame(ItemTypes.CHARCOAL, recipe.getResult().getType());
        assertNull(recipes.matchCookingRecipe(stem, Identifiers.FURNACE));
        assertEquals(300, items.requireComponent(log.getType(), ItemBehaviors.FUEL_DURATION).get());
        assertEquals(0, items.requireComponent(stem.getType(), ItemBehaviors.FUEL_DURATION).get());
    }

    @Test
    void registersCampfireRecipesForBothVariantsWithoutFurnaceExperience() {
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        ItemStack chicken = ItemStack.from(ItemTypes.CHICKEN);

        CloudCookingRecipe campfire = Objects.requireNonNull(recipes.matchCookingRecipe(chicken, Identifiers.CAMPFIRE));
        CloudCookingRecipe soulCampfire = Objects.requireNonNull(recipes.matchCookingRecipe(chicken, Identifiers.SOUL_CAMPFIRE));

        assertNotEquals(campfire.getId(), soulCampfire.getId());
        assertEquals(600, campfire.getCookingTime());
        assertEquals(600, soulCampfire.getCookingTime());
        assertSame(ItemTypes.COOKED_CHICKEN, campfire.getResult().getType());
        assertSame(ItemTypes.COOKED_CHICKEN, soulCampfire.getResult().getType());
        assertEquals(0f, campfire.getExperience());
        assertEquals(0f, soulCampfire.getExperience());
        assertNull(recipes.matchCookingRecipe(ItemStack.from(ItemTypes.RAW_IRON), Identifiers.CAMPFIRE));
    }
}
