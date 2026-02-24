package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.StonecutterView;

/**
 * Represents the stonecutter container screen.
 */
public interface StonecutterScreen extends ContainerScreen {

    StonecutterView getStonecutter();
}
