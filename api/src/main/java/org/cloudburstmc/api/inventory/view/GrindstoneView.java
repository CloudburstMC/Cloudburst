package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * The two inputs and result preview of an open grindstone.
 */
public interface GrindstoneView extends SlotGroup {

    /**
     * Returns the item in the upper input slot.
     *
     * @return the primary input item
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
     * @return the secondary input item
     */
    ItemStack getAdditional();

    /**
     * Sets the item in the lower input slot.
     *
     * @param item the item to place
     */
    void setAdditional(ItemStack item);

    /**
     * Returns the result preview. To change it, listen for
     * {@link org.cloudburstmc.api.event.inventory.PrepareGrindstoneEvent}.
     *
     * @return the result item
     */
    ItemStack getResult();
}
