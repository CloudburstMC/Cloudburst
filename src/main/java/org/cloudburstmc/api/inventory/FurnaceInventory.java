package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.Furnace;

public interface FurnaceInventory extends Inventory {

    @Override
    Furnace getHolder();
}
