package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Called when a furnace, blast furnace, or smoker starts a batch.
 */
public class FurnaceStartSmeltEvent extends InventoryBlockStartEvent {

    private final CookingRecipe recipe;
    private int totalCookTime;

    public FurnaceStartSmeltEvent(Block block, ItemStack source, CookingRecipe recipe) {
        super(block, source);
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.totalCookTime = recipe.getCookingTime();
    }

    /**
     * @return the recipe being started
     */
    public CookingRecipe getRecipe() {
        return this.recipe;
    }

    /**
     * @return the ticks required to complete this batch
     */
    public int getTotalCookTime() {
        return this.totalCookTime;
    }

    /**
     * @param ticks the cooking time for this batch
     * @throws IllegalArgumentException if the time is less than one tick
     */
    public void setTotalCookTime(int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("Cooking time must be positive");
        }
        this.totalCookTime = ticks;
    }
}
