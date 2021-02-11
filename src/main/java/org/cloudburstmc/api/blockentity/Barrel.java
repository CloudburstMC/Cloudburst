package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.BarrelInventory;
import org.cloudburstmc.api.inventory.InventoryHolder;

public interface Barrel extends BlockEntity, InventoryHolder {

    @Override
    BarrelInventory getInventory();

}
