package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockStonecutterView;

/**
 * Represents the stonecutter container screen.
 */
public interface StonecutterScreen extends ContainerScreen {

    /**
     * Returns the input and result slots of this stonecutter.
     */
    BlockStonecutterView getStonecutter();
}
