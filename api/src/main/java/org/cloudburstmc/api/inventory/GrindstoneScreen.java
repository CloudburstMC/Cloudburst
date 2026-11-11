package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.GrindstoneView;

/**
 * An open grindstone.
 */
public interface GrindstoneScreen extends ContainerScreen {

    /**
     * @return the grindstone inputs and result preview
     */
    GrindstoneView getGrindstone();
}
