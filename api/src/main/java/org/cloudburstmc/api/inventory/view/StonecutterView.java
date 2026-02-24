package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a stonecutter container.
 * Exposes the stone input slot and the result slot.
 */
public interface StonecutterView extends SlotGroup {

    /**
     * Returns the item in the input slot.
     *
     * @return the input item
     */
    ItemStack getInput();

    /**
     * Sets the item in the input slot.
     *
     * @param item the item to place
     */
    void setInput(ItemStack item);

    /**
     * Returns the item in the result slot.
     *
     * <p>This slot is computed by the client based on the selected recipe.
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
     * Returns the index of the recipe the player has selected in the stonecutter UI,
     * or {@code -1} if no recipe is currently selected.
     *
     * @return the selected recipe index, or {@code -1}
     */
    int getSelectedRecipeIndex();

    /**
     * Sets the selected recipe index shown in the stonecutter UI.
     * Use {@code -1} to clear the selection.
     *
     * @param index the recipe index, or {@code -1} to clear
     */
    void setSelectedRecipeIndex(int index);
}
