package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.server.inventory.BarrelInventory;
import org.cloudburstmc.server.inventory.InventoryHolder;

public interface Barrel extends BlockEntity, InventoryHolder {

    @Override
    BarrelInventory getInventory();
}
