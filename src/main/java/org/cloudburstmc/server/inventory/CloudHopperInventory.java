package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.Hopper;
import org.cloudburstmc.api.inventory.HopperInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.server.blockentity.HopperBlockEntity;

/**
 * Created by CreeperFace on 8.5.2017.
 */
public class CloudHopperInventory extends CloudContainer implements HopperInventory {

    public CloudHopperInventory(Hopper hopper) {
        super(hopper, InventoryType.HOPPER);
    }

    @Override
    public HopperBlockEntity getHolder() {
        return (HopperBlockEntity) super.getHolder();
    }
}
