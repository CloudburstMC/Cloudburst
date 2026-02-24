package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the 3×3 crafting grid of a crafting table block (9 slots, indices 0–8).
 *
 * <p>This interface is for the crafting table block, not the player's own 2×2 crafting grid.
 * The player 2×2 grid is represented by {@link CraftingView}.</p>
 *
 * <p>Slot layout (row-major order):
 * <pre>
 *   (0,0) (0,1) (0,2)
 *   (1,0) (1,1) (1,2)
 *   (2,0) (2,1) (2,2)
 * </pre>
 * Grid indices 0–8 in row-major order.
 * </p>
 *
 * @see CraftingView
 */
public interface CraftingTableView extends SlotGroup {

    /**
     * Returns the item at the given grid position.
     *
     * @param row row in 0..2
     * @param col column in 0..2
     * @return the item at that grid cell
     * @throws IndexOutOfBoundsException if row or col is out of range
     */
    default ItemStack getGridSlot(int row, int col) {
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            throw new IndexOutOfBoundsException("row and col must be in 0..2 for a 3x3 grid");
        }
        return getItem(row * 3 + col);
    }

    /**
     * Sets the item at the given grid position.
     *
     * @param row  row in 0..2
     * @param col  column in 0..2
     * @param item the item to place
     * @throws IndexOutOfBoundsException if row or col is out of range
     */
    default void setGridSlot(int row, int col, ItemStack item) {
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            throw new IndexOutOfBoundsException("row and col must be in 0..2 for a 3x3 grid");
        }
        setItem(row * 3 + col, item);
    }
}
