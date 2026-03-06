package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a brewing stand container.
 * Exposes the ingredient slot (item being brewed into potions),
 * the fuel slot (blaze powder), and the three potion bottle output slots,
 * as well as the brew and fuel progress counters.
 */
public interface BrewingStandView extends SlotGroup {

    ItemStack getIngredient();

    void setIngredient(ItemStack item);

    ItemStack getFuel();

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
     * Returns the number of ticks the current brew cycle has been running,
     * counting <em>upward</em> from {@code 0} (just started) to {@link #getBrewDuration()}
     * (brew complete).
     *
     * <p>Note: the underlying block entity stores a countdown timer internally,
     * but this method always returns the elapsed time (i.e. duration minus remaining),
     * so callers can treat it as a straightforward progress value.</p>
     *
     * @return brew progress in ticks, in the range {@code [0, getBrewDuration()]}
     */
    int getBrewProgress();

    /**
     * Sets the brew progress to the specified number of elapsed ticks.
     *
     * <p>The value is the elapsed time counting <em>upward</em> from {@code 0} to
     * {@link #getBrewDuration()}. Values are clamped to {@code [0, getBrewDuration()]}
     * by the implementation. Changing this value sends an immediate update to the client.</p>
     *
     * <p><strong>Inversion warning:</strong> the underlying block entity stores this as a
     * countdown (e.g. {@code 400 → 0}), so the implementation automatically inverts the
     * value when reading and writing. Always pass and read elapsed ticks (upward) through
     * this API; do not attempt to pass the raw countdown value directly.</p>
     *
     * @param ticks elapsed brew ticks (upward, {@code 0} = just started,
     *              {@link #getBrewDuration()} = complete)
     */
    void setBrewProgress(int ticks);

    /**
     * Returns the total number of ticks a single brew cycle takes.
     * For a standard brewing stand this is 400 (20 seconds at 20 TPS).
     *
     * @return brew duration in ticks
     */
    int getBrewDuration();

    /**
     * Returns the number of brew operations remaining in the current fuel charge
     * (0 to {@link #getMaxFuelLevel()}).
     *
     * @return current fuel level
     */
    int getFuelLevel();

    /**
     * Sets the current fuel level (number of brew operations remaining).
     *
     * <p>Changing this value sends an immediate update to the client.</p>
     *
     * @param level the new fuel level, typically in the range {@code [0, getMaxFuelLevel()]}
     */
    void setFuelLevel(int level);

    /**
     * Returns the maximum number of brew operations one blaze powder provides.
     * For a standard brewing stand this is 20.
     *
     * @return maximum fuel level
     */
    int getMaxFuelLevel();
}
