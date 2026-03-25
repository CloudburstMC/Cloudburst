package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * A crafting recipe where ingredients may be placed in any arrangement on the grid,
 * regardless of position or order.
 */
public interface ShapelessRecipe extends CraftingRecipe {

    /**
     * Returns the concrete item stacks for each ingredient slot.
     * For tag-based ingredients this returns the representative item. Use
     * {@link #getIngredientChoices()} to preserve full tag information.
     */
    List<? extends ItemStack> getIngredientList();

    /**
     * Returns the number of ingredient slots in this recipe.
     */
    int getIngredientCount();

    /**
     * Returns all ingredient descriptors in order, preserving tag information.
     * Use this instead of {@link #getIngredientList()} when you need to know whether an ingredient
     * accepts a tag rather than a single specific item.
     */
    List<RecipeIngredient> getIngredientChoices();
}
