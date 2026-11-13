package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A recipe that converts one ingredient into a cooked result at the station returned by {@link #getBlock()}.
 */
public interface CookingRecipe extends Recipe {

    /**
     * @return the ingredient this recipe accepts
     */
    RecipeIngredient getInput();

    /**
     * @return an example ingredient stack
     * @see #getInput()
     */
    ItemStack getInputItem();

    /**
     * @return the match priority, with lower values preferred
     */
    int getPriority();

    /**
     * @return the number of ticks required to cook one batch
     */
    int getCookingTime();

    /**
     * @return the experience earned for one completed batch, before rounding
     */
    float getExperience();
}
