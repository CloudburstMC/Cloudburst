package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockFurnaceView;

/**
 * Represents an open smelting container screen
 * (furnace, blast furnace, or smoker).
 */
public interface FurnaceScreen extends ContainerScreen {

    /**
     * Returns the section giving access to the smelting, fuel, and result slots.
     *
     * @return the furnace section
     */
    BlockFurnaceView getFurnace();
}
