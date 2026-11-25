package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.EnderChest;
import org.cloudburstmc.api.inventory.view.EnderChestView;

/**
 * A screen for the player's personal ender inventory.
 */
public interface EnderChestScreen extends ContainerScreen {

    /**
     * Returns the block used to open this screen.
     */
    EnderChest getChest();

    /**
     * Returns the player's 27-slot ender inventory.
     */
    EnderChestView getEnderChest();
}
