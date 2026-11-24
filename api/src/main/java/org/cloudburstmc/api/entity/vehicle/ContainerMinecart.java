package org.cloudburstmc.api.entity.vehicle;

import org.cloudburstmc.api.inventory.view.StorageView;

/**
 * A minecart with persistent item storage.
 */
public interface ContainerMinecart extends Minecart {

    /**
     * Returns the live view of this minecart's storage.
     */
    StorageView getInventory();
}
