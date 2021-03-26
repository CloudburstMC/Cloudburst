package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.ShulkerBox;

public interface ShulkerBoxInventory extends ContainerInventory {

    @Override
    ShulkerBox getHolder();

}
