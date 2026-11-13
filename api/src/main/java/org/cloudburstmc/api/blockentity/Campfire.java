package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.item.ItemStack;

/**
 * A campfire with independent cooking slots.
 * Slot indices range from zero to {@link #getSize()} exclusive.
 */
public interface Campfire extends BlockEntity {

    /**
     * @return the number of cooking slots
     */
    int getSize();

    /**
     * @return the ingredient in the given slot, or an empty stack
     */
    ItemStack getItem(int slot);

    /**
     * Replaces the ingredient and resets its cooking progress.
     *
     * @param slot the cooking slot
     * @param item one cookable item, or {@link ItemStack#EMPTY} to clear the slot
     * @throws IllegalArgumentException if the stack is not empty and is not one cookable item
     */
    void setItem(int slot, ItemStack item);

    /**
     * @return elapsed cooking ticks in the given slot
     */
    int getCookProgress(int slot);

    /**
     * Sets elapsed cooking ticks, clamped between zero and the slot's cook duration.
     */
    void setCookProgress(int slot, int ticks);

    /**
     * @return total cooking ticks in the given slot, or zero if it is empty
     */
    int getCookDuration(int slot);

    /**
     * Sets the cook duration of an occupied slot.
     *
     * @throws IllegalArgumentException if the duration is less than one tick
     * @throws IllegalStateException    if the slot is empty
     */
    void setCookDuration(int slot, int ticks);

    /**
     * @return whether cooking is enabled for this slot, regardless of whether the campfire is lit
     */
    boolean isCookingEnabled(int slot);

    /**
     * Enables or pauses cooking in the slot without clearing its ingredient.
     */
    void setCookingEnabled(int slot, boolean enabled);

    /**
     * Adds one item to the first free slot without changing the supplied stack.
     *
     * @return whether the ingredient was added
     */
    boolean addItem(ItemStack item);
}
