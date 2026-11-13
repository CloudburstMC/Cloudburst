package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * The slots and progress of a furnace, blast furnace, or smoker.
 */
public interface FurnaceView extends SlotGroup {

    /**
     * @return the ingredient, or an empty stack
     */
    ItemStack getSmelting();

    /**
     * @param item the new ingredient, or {@link ItemStack#EMPTY} to clear the slot
     */
    void setSmelting(ItemStack item);

    /**
     * @return the fuel slot contents, or an empty stack
     */
    ItemStack getFuel();

    /**
     * @param item the new fuel slot contents, or {@link ItemStack#EMPTY} to clear the slot
     */
    void setFuel(ItemStack item);

    /**
     * @return the output slot contents, or an empty stack
     */
    ItemStack getResult();

    /**
     * @param item the new output, or {@link ItemStack#EMPTY} to clear the slot
     */
    void setResult(ItemStack item);

    /**
     * Returns the number of ticks the current item has been cooking (0 to {@link #getCookDuration()}).
     *
     * @return cook progress in ticks
     */
    int getCookProgress();

    /**
     * Sets the cook progress, clamped to the current cook duration.
     *
     * @param ticks cook progress in ticks
     */
    void setCookProgress(int ticks);

    /**
     * Returns the cook duration for the current item, or the furnace default if no recipe matches.
     *
     * @return cook duration in ticks
     */
    int getCookDuration();

    /**
     * Returns the number of ticks of fuel remaining in the current burn cycle
     * (0 to {@link #getBurnDuration()}).
     *
     * @return remaining burn ticks
     */
    int getBurnProgress();

    /**
     * Sets the remaining burn time. Negative values are treated as zero.
     *
     * @param ticks remaining burn ticks
     */
    void setBurnProgress(int ticks);

    /**
     * Returns the total number of ticks the current fuel item provided when it started burning.
     *
     * @return burn duration in ticks
     */
    int getBurnDuration();

    /**
     * Sets the total burn duration. It cannot be less than the remaining burn time.
     *
     * @param ticks total burn duration in ticks
     */
    void setBurnDuration(int ticks);
}
