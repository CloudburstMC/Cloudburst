package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.ContainerInventory;

public interface ShulkerBox extends BlockEntity, ContainerBlockEntity {

    @Override
    ContainerInventory getInventory();
}
