package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.Hopper;
import org.cloudburstmc.api.inventory.InventoryType;

/**
 * Created by CreeperFace on 8.5.2017.
 */
public class HopperInventory extends ContainerInventory {

    public HopperInventory(Hopper hopper) {
        super(hopper, InventoryType.HOPPER);
    }

    @Override
    public Hopper getHolder() {
        return (Hopper) super.getHolder();
    }
}
