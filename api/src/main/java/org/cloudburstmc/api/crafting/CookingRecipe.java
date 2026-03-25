package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A recipe processed by a cooking station (furnace, blast furnace, smoker, campfire).
 * The specific station is identified by {@link #getBlock()}.
 */
public interface CookingRecipe extends Recipe {

    /**
     * Returns the ingredient descriptor for the input slot.
     * Can be a tag-based ingredient for recipes that accept a category of items.
     */
    RecipeIngredient getInput();

    /**
     * Returns a representative {@link ItemStack} for the input slot.
     * Useful for display and item-based recipe matching.
     * For tag-based inputs this returns a representative concrete item that satisfies the tag.
     */
    ItemStack getInputItem();

    /**
     * Returns the priority used to order this recipe in the recipe book display.
     * Lower values are preferred over higher values when multiple cooking recipes match the same input.
     */
    int getPriority();
}
