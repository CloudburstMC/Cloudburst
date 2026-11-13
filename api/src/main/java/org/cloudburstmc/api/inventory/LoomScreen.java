package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockLoomView;

/**
 * Represents the loom container screen.
 */
public interface LoomScreen extends ContainerScreen {

    /**
     * Returns the input and result slots for the loom.
     */
    BlockLoomView getLoom();
}
