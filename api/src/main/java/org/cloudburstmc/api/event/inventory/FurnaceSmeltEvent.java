package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.event.block.BlockCookEvent;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Called when a furnace, blast furnace, or smoker completes a batch.
 */
public class FurnaceSmeltEvent extends BlockCookEvent {

    public FurnaceSmeltEvent(Block block, ItemStack source, ItemStack result, CookingRecipe recipe) {
        super(block, source, result, recipe);
    }
}
