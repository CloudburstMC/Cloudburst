package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A recipe processed by the stonecutter.
 */
public interface StonecuttingRecipe extends Recipe {

    /**
     * Returns the ingredient descriptor for the input slot.
     * Can be a tag-based ingredient for recipes that accept a category of items.
     */
    RecipeIngredient getInput();

    /**
     * Returns a representative {@link ItemStack} for the input slot.
     * Useful for display and item-based recipe matching.
     * A tag-based input may have no representative item and return an empty stack.
     */
    ItemStack getInputItem();

    /**
     * Returns the priority used to order this recipe's output in the stonecutter display.
     * Lower values are preferred over higher values when multiple outputs are available for the same input.
     */
    int getPriority();
}
