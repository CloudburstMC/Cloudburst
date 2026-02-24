package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.AnvilView;

/**
 * Represents an open anvil screen.
 */
public interface AnvilScreen extends ContainerScreen {

    /**
     * Returns the section giving access to the input, material, result, and repair cost.
     *
     * @return the anvil section
     */
    AnvilView getAnvil();
}
