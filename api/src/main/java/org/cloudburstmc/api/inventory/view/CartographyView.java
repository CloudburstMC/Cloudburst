package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a cartography table container.
 * Exposes the map input, the additional item (paper or another map), and the result slots.
 */
public interface CartographyView extends SlotGroup {

    /**
     * Returns the item in the map input slot.
     *
     * @return the map input item
     */
    ItemStack getInput();

    /**
     * Sets the item in the map input slot.
     *
     * @param item the item to place
     */
    void setInput(ItemStack item);

    /**
     * Returns the item in the additional slot (e.g. paper to extend, or another map to clone).
     *
     * @return the additional item
     */
    ItemStack getAdditional();

    /**
     * Sets the item in the additional slot.
     *
     * @param item the item to place
     */
    void setAdditional(ItemStack item);

    /**
     * Returns the item in the result slot.
     *
     * <p>This slot is computed by the client from the map and additional inputs.
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
