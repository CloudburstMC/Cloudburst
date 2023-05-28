package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.Chest;

public interface ChestInventory extends ContainerInventory {

    @Override
    Chest getHolder();

    boolean isDouble();
}
