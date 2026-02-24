package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.CartographyView;

/**
 * Represents the cartography table container screen.
 */
public interface CartographyScreen extends ContainerScreen {

    CartographyView getCartography();
}
