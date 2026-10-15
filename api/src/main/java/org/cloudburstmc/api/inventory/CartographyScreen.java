package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.CartographyView;

/**
 * Represents the cartography table container screen.
 */
public interface CartographyScreen extends ContainerScreen {

    /**
     * @return the two input slots and the result slot
     */
    CartographyView getCartography();
}
