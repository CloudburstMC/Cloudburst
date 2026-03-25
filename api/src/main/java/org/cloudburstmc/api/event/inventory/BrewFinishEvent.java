package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired when a brewing cycle completes in a {@link BrewingStand}. Carries the reagent,
 * the three brewed output slots, and the remaining fuel level.
 *
 * <p>Override any output slot via {@link #setPotion(int, ItemStack)} before the results
 * are written back. Cancelling this event prevents all outputs from being produced.
 */
public final class BrewFinishEvent extends Event implements Cancellable {

    private final BrewingStand brewingStand;
    private final ItemStack ingredient;
    private final ItemStack[] potions;
    private final int fuel;

    public BrewFinishEvent(BrewingStand brewingStand, ItemStack[] potions) {
        this.brewingStand = brewingStand;
        this.fuel = brewingStand.getFuelLevel();
        this.ingredient = brewingStand.getIngredient();
        this.potions = potions.clone();
    }

    /**
     * Returns the brewing stand where this cycle completed.
     */
    public BrewingStand getBrewingStand() {
        return brewingStand;
    }

    /**
     * Returns the reagent item that was consumed during this brewing cycle.
     */
    public ItemStack getIngredient() {
        return ingredient;
    }

    /**
     * Returns a snapshot of all three brewed output slots, indexed 0 to 2.
     */
    public ItemStack[] getPotions() {
        return potions.clone();
    }

    /**
     * Returns the brewed output for the given bottle slot.
     *
     * @param index bottle slot index, 0 to 2
     */
    public ItemStack getPotion(int index) {
        return this.potions[index];
    }

    /**
     * Overrides the brewed output for the given bottle slot.
     *
     * @param index bottle slot index, 0 to 2
     * @param item  the replacement item
     */
    public void setPotion(int index, ItemStack item) {
        if (index < 0 || index > 2) {
            throw new IndexOutOfBoundsException("Potion index must be 0-2, got: " + index);
        }
        this.potions[index] = item;
    }

    /**
     * Returns the remaining fuel level after this brewing cycle completes.
     */
    public int getFuel() {
        return fuel;
    }
}
