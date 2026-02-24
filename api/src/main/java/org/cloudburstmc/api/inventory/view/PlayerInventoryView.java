package org.cloudburstmc.api.inventory.view;

/**
 * Represents the player's full 36-slot main inventory (slots 0–35), where slots
 * 0–8 are also the hotbar (mirrored by {@link HotbarView}).
 *
 * <p>Convenience methods for hotbar selection are provided via {@link SelectedSlotAccess}
 * so callers do not need to fetch the separate {@link HotbarView} for common operations.
 * Use {@link org.cloudburstmc.api.player.Player#getHotbar()} when you specifically
 * need the 9-slot hotbar section.</p>
 */
public interface PlayerInventoryView extends PlayerSlotGroup, SelectedSlotAccess {

    /**
     * {@inheritDoc}
     *
     * <p>Always returns 9 for the player inventory.</p>
     */
    @Override
    default int getHotbarSize() {
        return 9;
    }
}
