package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a grindstone container.
 * Exposes the two input slots and the result slot.
 */
public interface GrindstoneView extends SlotGroup {

    /**
     * Returns the item in the primary (left) input slot.
     *
     * @return the primary input item
     */
    ItemStack getInput();

    /**
     * Sets the item in the primary (left) input slot.
     *
     * @param item the item to place
     */
    void setInput(ItemStack item);

    /**
     * Returns the item in the secondary (right) input slot.
     *
     * @return the secondary input item
     */
    ItemStack getAdditional();

    /**
     * Sets the item in the secondary (right) input slot.
     *
     * @param item the item to place
     */
    void setAdditional(ItemStack item);

    /**
     * Returns the item in the result slot.
     *
     * <p>This slot is computed by the client from the two inputs.
     * The server can read or override the result by setting this slot directly.</p>
     *
     * @return the result item
     */
    ItemStack getResult();

    /**
     * Sets the item in the result slot.
     *
     * @param item the result item to place
     */
    void setResult(ItemStack item);
}
