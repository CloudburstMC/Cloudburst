package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.block.BlockEvent;

/**
 * Fired when a furnace begins smelting a new item. This fires once at the start of each smelt cycle,
 * before any cook progress is accumulated.
 *
 * <p>Change the total cook duration via {@link #setCookTime(int)}, which controls how many
 * ticks the item takes to finish smelting. Cancelling this event prevents the smelt from starting.
 */
public final class FurnaceStartSmeltEvent extends BlockEvent implements Cancellable {

    private final Furnace furnace;
    private final CookingRecipe recipe;
    private int cookTime;

    public FurnaceStartSmeltEvent(Furnace furnace, CookingRecipe recipe, int cookTime) {
        super(furnace.getBlock());
        this.furnace = furnace;
        this.recipe = recipe;
        this.cookTime = cookTime;
    }

    /**
     * Returns the furnace that is starting the smelt cycle.
     */
    public Furnace getFurnace() {
        return furnace;
    }

    /**
     * Returns the recipe that will be smelted.
     */
    public CookingRecipe getRecipe() {
        return recipe;
    }

    /**
     * Returns the total number of ticks required to complete this smelt cycle.
     */
    public int getCookTime() {
        return cookTime;
    }

    /**
     * Overrides the total cook duration for this smelt cycle.
     * The value must be at least 1.
     *
     * @param cookTime ticks to smelt, must be at least 1
     */
    public void setCookTime(int cookTime) {
        if (cookTime < 1) {
            throw new IllegalArgumentException("cookTime must be at least 1, got: " + cookTime);
        }
        this.cookTime = cookTime;
    }
}
