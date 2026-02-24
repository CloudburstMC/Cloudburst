package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.CursorView;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Mixin interface for any inventory screen that exposes the player's cursor slot.
 *
 * <p>Both {@link ContainerScreen} (open container windows) and
 * {@link PlayerInventoryScreen} (the player's own inventory screen) expose a cursor,
 * so plugins can access the cursor without needing to cast to a specific screen type:</p>
 *
 * <pre>{@code
 * if (screen instanceof CursorAccess ca) {
 *     ItemStack held = ca.getCursorItem();
 * }
 * }</pre>
 */
public interface CursorAccess {

    /**
     * Returns the cursor slot group — the raw {@link CursorView} backing the cursor.
     * Prefer {@link #getCursorItem()} / {@link #setCursorItem(ItemStack)} for simple
     * item access.
     *
     * @return the cursor slot group
     */
    CursorView getCursor();

    /**
     * Returns the item currently held on the player's mouse cursor.
     *
     * @return the cursor item
     */
    default ItemStack getCursorItem() {
        return getCursor().getCursor();
    }

    /**
     * Sets the item currently held on the player's mouse cursor.
     *
     * @param item the item to place on the cursor
     */
    default void setCursorItem(ItemStack item) {
        getCursor().setCursor(item);
    }
}
