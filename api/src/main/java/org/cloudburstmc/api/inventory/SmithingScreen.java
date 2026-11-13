package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.SmithingView;

/**
 * An open smithing table.
 */
public interface SmithingScreen extends ContainerScreen {

    /**
     * @return the smithing inputs and result preview
     */
    SmithingView getSmithing();
}
