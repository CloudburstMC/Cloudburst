package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A brewing stand recipe where one input item and one reagent combine to produce one output item.
 *
 * <p>Used for two distinct recipe categories, distinguished by {@link #getType()}:
 * <ul>
 *   <li>{@link RecipeType#POTION}: potion brewing, combining a base potion with a reagent item.
 *   <li>{@link RecipeType#POTION_CONTAINER}: container transformation, changing the container type of the potion
 *       (e.g. converting a regular potion into a splash potion).
 * </ul>
 * Both use the brewing stand as their station.
 */
public interface MixRecipe extends Recipe {

    /**
     * Returns the base input item for this recipe.
     * Always a specific item stack rather than a tag-based ingredient.
     */
    ItemStack getInput();

    /**
     * Returns the reagent item placed in the top slot of the brewing stand.
     * Always a specific item stack rather than a tag-based ingredient.
     */
    ItemStack getIngredient();
}
