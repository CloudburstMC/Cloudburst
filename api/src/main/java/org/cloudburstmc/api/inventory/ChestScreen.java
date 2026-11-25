package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.Chest;

/**
 * A screen for a single or double chest.
 */
public interface ChestScreen extends StorageScreen {

    /**
     * Returns the chest half used to open this screen.
     */
    Chest getChest();
}
