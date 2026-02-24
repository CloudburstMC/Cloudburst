package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.CraftingTableView;

/**
 * Represents the crafting table container screen.
 */
public interface CraftingTableScreen extends ContainerScreen {

    /**
     * Returns the 3×3 crafting grid of the crafting table.
     *
     * @return the crafting table slot group
     */
    CraftingTableView getCraftingTable();
}
