package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * A recipe crafted in a crafting grid, either the 2x2 inventory grid or a 3x3 crafting table.
 * Subtypes include {@link ShapedRecipe}, {@link ShapelessRecipe}, and {@link ShulkerBoxRecipe}.
 */
public interface CraftingRecipe extends Recipe {

    /**
     * Returns {@code true} if this recipe requires a 3x3 crafting grid (crafting table).
     * Returns {@code false} if it can be crafted in the 2x2 inventory grid.
     */
    boolean requiresCraftingTable();

    /**
     * Returns the secondary output items produced alongside the primary result.
     * Does not include the primary result ({@link #getResult()}).
     * Returns an empty list if there are no secondary outputs.
     */
    List<? extends ItemStack> getExtraResults();

    /**
     * Returns all output items: the primary result followed by any secondary outputs.
     * Equivalent to prepending {@link #getResult()} to {@link #getExtraResults()}.
     */
    List<? extends ItemStack> getAllResults();

    /**
     * Returns the priority used to order this recipe in the recipe book display.
     * Lower values are preferred over higher values when multiple recipes share the same inputs.
     */
    int getPriority();

    /**
     * Returns the context under which this recipe is automatically unlocked in the recipe book.
     * Defaults to {@link RecipeUnlockContext#NONE}.
     */
    default RecipeUnlockContext getUnlockContext() {
        return RecipeUnlockContext.NONE;
    }
}
