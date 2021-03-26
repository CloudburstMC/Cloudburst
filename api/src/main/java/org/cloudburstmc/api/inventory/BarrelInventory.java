package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.Barrel;

public interface BarrelInventory extends ContainerInventory {

    @Override
    Barrel getHolder();
}
