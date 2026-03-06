package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the player's 2×2 crafting grid slots inside the player inventory screen.
 *
 * <p>This slot group is always attached to a player, not a block; the 2×2 crafting
 * grid is part of the player's own inventory UI.</p>
 *
 * <p>Slot layout (row-major order):
 * <pre>
 *   (0,0) (0,1)
 *   (1,0) (1,1)
 * </pre>
 * Grid indices 0–3 in row-major order.
 * </p>
 */
public interface CraftingView extends PlayerSlotGroup {

    /**
     * Returns the item at the given grid position.
     *
     * @param row row in 0..1
     * @param col column in 0..1
     * @return the item at that grid cell
     * @throws IndexOutOfBoundsException if row or col is out of range
     */
    default ItemStack getGridSlot(int row, int col) {
        if (row < 0 || row > 1 || col < 0 || col > 1) {
            throw new IndexOutOfBoundsException("row and col must be in 0..1 for a 2x2 grid");
        }
        return getItem(row * 2 + col);
    }

    /**
     * Sets the item at the given grid position.
     *
     * @param row  row in 0..1
     * @param col  column in 0..1
     * @param item the item to place
     * @throws IndexOutOfBoundsException if row or col is out of range
     */
    default void setGridSlot(int row, int col, ItemStack item) {
        if (row < 0 || row > 1 || col < 0 || col > 1) {
            throw new IndexOutOfBoundsException("row and col must be in 0..1 for a 2x2 grid");
        }
        setItem(row * 2 + col, item);
    }
}
