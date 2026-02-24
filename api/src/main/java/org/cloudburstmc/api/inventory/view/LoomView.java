package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a loom container.
 * Exposes the banner, dye, pattern material, and result slots.
 */
public interface LoomView extends SlotGroup {

    /**
     * Returns the item in the banner input slot.
     *
     * @return the banner item
     */
    ItemStack getBanner();

    /**
     * Sets the item in the banner input slot.
     *
     * @param item the item to place
     */
    void setBanner(ItemStack item);

    /**
     * Returns the item in the dye slot.
     *
     * @return the dye item
     */
    ItemStack getDye();

    /**
     * Sets the item in the dye slot.
     *
     * @param item the item to place
     */
    void setDye(ItemStack item);

    /**
     * Returns the item in the pattern material slot (banner pattern item).
     *
     * @return the pattern material item
     */
    ItemStack getPattern();

    /**
     * Sets the item in the pattern material slot.
     *
     * @param item the item to place
     */
    void setPattern(ItemStack item);

    /**
     * Returns the item in the result slot.
     *
     * <p>This slot is computed by the client from the banner, dye, and pattern inputs.
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

    /**
     * Returns the index of the banner pattern the player has selected in the loom pattern list,
     * or {@code -1} if no pattern is selected.
     *
     * @return the selected pattern index, or {@code -1}
     */
    int getSelectedPatternIndex();

    /**
     * Sets the index of the banner pattern selected in the loom pattern list.
     * Pass {@code -1} to clear the selection.
     *
     * @param index the pattern index, or {@code -1} to clear
     */
    void setSelectedPatternIndex(int index);
}
