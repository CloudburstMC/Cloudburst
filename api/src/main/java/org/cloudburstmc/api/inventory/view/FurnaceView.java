package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a smelting container (furnace, blast furnace, or smoker).
 * Exposes the three logical slots: smelting ingredient, fuel, and result,
 * as well as the cook and burn progress counters.
 */
public interface FurnaceView extends SlotGroup {

    ItemStack getSmelting();

    void setSmelting(ItemStack item);

    ItemStack getFuel();

    void setFuel(ItemStack item);

    ItemStack getResult();

    void setResult(ItemStack item);

    /**
     * Returns the number of ticks the current item has been cooking (0 to {@link #getCookDuration()}).
     *
     * @return cook progress in ticks
     */
    int getCookProgress();

    /**
     * Sets the number of ticks the current item has been cooking.
     *
     * <p>Values outside the range {@code [0, getCookDuration()]} are clamped by the
     * implementation. Changing this value sends an immediate update to the client.</p>
     *
     * @param ticks cook progress in ticks
     */
    void setCookProgress(int ticks);

    /**
     * Returns the total number of ticks required to smelt the current item.
     * For a standard furnace this is 200 (10 seconds at 20 TPS).
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
     * Sets the number of ticks of fuel remaining in the current burn cycle.
     *
     * <p>Changing this value sends an immediate update to the client.</p>
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
     * Sets the total burn duration for the current fuel item.
     *
     * <p>This is the denominator of the burn-progress bar. Changing this value sends an
     * immediate update to the client.</p>
     *
     * @param ticks total burn duration in ticks
     */
    void setBurnDuration(int ticks);
}
