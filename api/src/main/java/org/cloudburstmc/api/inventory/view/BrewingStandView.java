package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * The ingredient, fuel, bottle slots, and progress of a brewing stand.
 */
public interface BrewingStandView extends SlotGroup {

    /**
     * @return the ingredient, or an empty stack
     */
    ItemStack getIngredient();

    /**
     * @param item the ingredient to place, or an empty stack to clear the slot
     */
    void setIngredient(ItemStack item);

    /**
     * @return the fuel item, or an empty stack
     */
    ItemStack getFuel();

    /**
     * @param item the fuel to place, or an empty stack to clear the slot
     */
    void setFuel(ItemStack item);

    /**
     * Returns the potion bottle in the given slot (0–2).
     *
     * @param slot the bottle slot index, 0–2
     * @return the item in the given bottle slot
     * @throws IndexOutOfBoundsException if {@code slot} is not 0–2
     */
    ItemStack getBottle(int slot);

    /**
     * Sets the potion bottle in the given slot (0–2).
     *
     * @param slot the bottle slot index, 0–2
     * @param item the item to place in the slot
     * @throws IndexOutOfBoundsException if {@code slot} is not 0–2
     */
    void setBottle(int slot, ItemStack item);

    /**
     * Returns the potion bottle in the left slot (slot 0).
     */
    default ItemStack getBottleLeft() {
        return getBottle(0);
    }

    /**
     * Sets the potion bottle in the left slot (slot 0).
     */
    default void setBottleLeft(ItemStack item) {
        setBottle(0, item);
    }

    /**
     * Returns the potion bottle in the middle slot (slot 1).
     */
    default ItemStack getBottleMiddle() {
        return getBottle(1);
    }

    /**
     * Sets the potion bottle in the middle slot (slot 1).
     */
    default void setBottleMiddle(ItemStack item) {
        setBottle(1, item);
    }

    /**
     * Returns the potion bottle in the right slot (slot 2).
     */
    default ItemStack getBottleRight() {
        return getBottle(2);
    }

    /**
     * Sets the potion bottle in the right slot (slot 2).
     */
    default void setBottleRight(ItemStack item) {
        setBottle(2, item);
    }

    /**
     * @return elapsed ticks in the active batch, or zero when idle
     */
    int getBrewProgress();

    /**
     * Changes the progress of an active batch. This has no effect while idle.
     *
     * @param ticks elapsed ticks, clamped so the batch cannot complete before the next tick
     */
    void setBrewProgress(int ticks);

    /**
     * @return the active batch's duration, or the default duration while idle
     */
    int getBrewDuration();

    /**
     * @return the number of batches remaining in the current fuel charge
     */
    int getFuelLevel();

    /**
     * @param level the number of batches to make available, clamped to zero if negative
     */
    void setFuelLevel(int level);

    /**
     * @return the default number of batches supplied by one blaze powder
     */
    int getMaxFuelLevel();
}
