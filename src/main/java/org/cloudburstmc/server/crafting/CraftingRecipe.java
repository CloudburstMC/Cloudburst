package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * @author CreeperFace
 */
public interface CraftingRecipe extends Recipe {

    boolean requiresCraftingTable();

    List<ItemStack> getExtraResults();

    List<ItemStack> getAllResults();

    int getPriority();

    /**
     * Returns whether the specified list of crafting grid inputs and outputs matches this recipe. Outputs DO NOT
     * include the primary result item.
     *
     * @param input  2D array of items taken from the crafting grid
     * @param output 2D array of items put back into the crafting grid (secondary results)
     * @return bool
     */
    boolean matchItems(ItemStack[][] input, ItemStack[][] output);
}
