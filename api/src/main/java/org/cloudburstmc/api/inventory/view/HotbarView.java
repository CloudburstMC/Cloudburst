package org.cloudburstmc.api.inventory.view;

/**
 * Represents the player's hotbar — the 9-slot quick-access bar (slots 0–8).
 *
 * <p>The hotbar mirrors the first 9 slots of the player's main
 * {@link PlayerInventoryView}. Use this interface when you need hotbar-specific
 * semantics such as the selected slot or selected item.</p>
 */
public interface HotbarView extends PlayerSlotGroup, SelectedSlotAccess {

    /**
     * {@inheritDoc}
     *
     * <p>For the hotbar, this is always equal to {@link #size()} (always 9).</p>
     */
    @Override
    default int getHotbarSize() {
        return size();
    }
}
