package org.cloudburstmc.api.crafting;

/**
 * A recipe that recolors a shulker box by combining it with a dye.
 */
public interface ShulkerBoxRecipe extends CraftingRecipe {

    /**
     * Returns the ingredient descriptor for the shulker box slot.
     */
    RecipeIngredient getShulkerBox();

    /**
     * Returns the ingredient descriptor for the dye slot.
     */
    RecipeIngredient getDye();
}
