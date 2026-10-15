package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a cartography table container.
 * Recipes accept ingredients in either input slot.
 */
public interface CartographyView extends SlotGroup {

    /**
     * Returns the item in the upper input slot.
     *
     * @return the input item, or an empty stack
     */
    ItemStack getInput();

    /**
     * Sets the item in the upper input slot.
     *
     * @param item the item to place
     */
    void setInput(ItemStack item);

    /**
     * Returns the item in the lower input slot.
     *
     * @return the input item, or an empty stack
     */
    ItemStack getAdditional();

    /**
     * Sets the item in the lower input slot.
     *
     * @param item the item to place
     */
    void setAdditional(ItemStack item);

    /**
     * Returns the item in the result slot.
     *
     * <p>The result is calculated from the current inputs. Use
     * {@link org.cloudburstmc.api.event.inventory.PrepareResultEvent} to customize it.</p>
     *
     * @return the result item, or an empty stack when no result is available
     */
    ItemStack getResult();

    /**
     * Sets the item in the result slot.
     * Input changes can replace this value. Use
     * {@link org.cloudburstmc.api.event.inventory.PrepareResultEvent} to customize a takeable result.
     *
     * @param item the result item to place
     */
    void setResult(ItemStack item);
}
