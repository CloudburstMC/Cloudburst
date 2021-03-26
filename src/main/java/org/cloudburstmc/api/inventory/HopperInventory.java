package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.Hopper;

public interface HopperInventory extends ContainerInventory {

    @Override
    Hopper getHolder();

}
