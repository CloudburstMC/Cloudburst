package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.EnderChestView;

/**
 * Represents an open ender chest container screen.
 *
 * <p>The ender chest shows the player's personal 27-slot ender storage.
 * Its contents are per-player and not tied to the block itself.</p>
 */
public interface EnderChestScreen extends ContainerScreen {

    /**
     * Returns the 27-slot ender chest storage view.
     *
     * @return the ender chest storage slot group
     */
    EnderChestView getEnderChest();
}
