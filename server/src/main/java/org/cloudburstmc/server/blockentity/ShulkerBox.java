package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.inventory.ShulkerBoxInventory;

public interface ShulkerBox extends BlockEntity, ContainerBlockEntity {

    @Override
    ShulkerBoxInventory getInventory();
}
