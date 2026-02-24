package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.SmithingView;

/**
 * Represents the smithing table container screen.
 */
public interface SmithingScreen extends ContainerScreen {

    SmithingView getSmithing();
}
