package org.cloudburstmc.api.entity.vehicle;

import org.cloudburstmc.api.inventory.view.StorageView;

/**
 * A single-passenger boat or raft with persistent item storage.
 */
public interface ChestBoat extends Boat {

    /**
     * @return the boat's editable 27-slot inventory
     */
    StorageView getInventory();
}
