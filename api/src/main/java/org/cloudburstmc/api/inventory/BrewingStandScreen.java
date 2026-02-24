package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockBrewingStandView;

/**
 * Represents an open brewing stand screen.
 */
public interface BrewingStandScreen extends ContainerScreen {

    /**
     * Returns the section giving access to the ingredient and fuel slots.
     *
     * @return the brewing stand section
     */
    BlockBrewingStandView getBrewingStand();
}
