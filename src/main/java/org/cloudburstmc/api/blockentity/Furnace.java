package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.FurnaceInventory;

public interface Furnace extends BlockEntity, ContainerBlockEntity {

    @Override
    FurnaceInventory getInventory();
}
