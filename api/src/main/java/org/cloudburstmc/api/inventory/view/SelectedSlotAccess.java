package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Provides access to the currently selected hotbar slot and its item.
 *
 * <p>Implemented by both {@link HotbarView} (the 9-slot hotbar section) and
 * {@link PlayerInventoryView} (the full 36-slot main inventory) so callers
 * can read/write selected-slot state without needing to know which view they hold.</p>
 */
public interface SelectedSlotAccess {

    /**
     * Returns the item currently held in the selected hotbar slot.
     *
     * @return the selected item, never {@code null}
     */
    @NonNull
    ItemStack getSelectedItem();

    /**
     * Sets the item in the currently selected hotbar slot.
     *
     * @param item the item to place
     */
    void setSelectedItem(ItemStack item);

    /**
     * Returns the index of the currently selected hotbar slot (0–8).
     *
     * @return the selected slot index
     */
    int getSelectedSlot();

    /**
     * Sets the currently selected hotbar slot.
     *
     * @param slot the slot index (0–8)
     */
    void setSelectedSlot(int slot);

    /**
     * Returns the number of hotbar slots (always 9).
     *
     * @return 9
     */
    int getHotbarSize();
}
