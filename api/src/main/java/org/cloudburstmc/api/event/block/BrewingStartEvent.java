package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired before a brewing batch starts.
 */
public class BrewingStartEvent extends BlockEvent {

    private final ItemStack source;
    private final int recipeBrewTime;
    private int brewingTime;

    public BrewingStartEvent(BrewingStand stand, ItemStack source, int recipeBrewTime) {
        super(stand.getBlock());
        this.source = source;
        this.recipeBrewTime = recipeBrewTime;
        this.setBrewingTime(recipeBrewTime);
    }

    /**
     * @return the ingredient being brewed
     */
    public ItemStack getSource() {
        return this.source;
    }

    /**
     * @return the original duration in ticks
     */
    public int getRecipeBrewTime() {
        return this.recipeBrewTime;
    }

    /**
     * @return the duration this batch will use, in ticks
     */
    public int getBrewingTime() {
        return this.brewingTime;
    }

    /**
     * @param ticks the positive duration of this batch
     */
    public void setBrewingTime(int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Brewing time must be positive");
        }
        this.brewingTime = ticks;
    }
}
