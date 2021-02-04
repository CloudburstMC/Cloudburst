package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.server.inventory.ShulkerBoxInventory;

public interface ShulkerBox extends BlockEntity, ContainerBlockEntity {

    @Override
    ShulkerBoxInventory getInventory();
}
