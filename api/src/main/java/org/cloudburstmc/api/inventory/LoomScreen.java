package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.LoomView;

/**
 * Represents the loom container screen.
 */
public interface LoomScreen extends ContainerScreen {

    LoomView getLoom();
}
