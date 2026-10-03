package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.inventory.view.StorageView;

/**
 * A storage screen backed by an entity's persistent inventory.
 */
public interface EntityStorageScreen extends ContainerScreen {

    /**
     * @return the entity that owns the inventory
     */
    Entity getEntity();

    /**
     * @return the entity's storage slots
     */
    StorageView getStorage();
}
