package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudArmorInventory extends CloudInventory {

    public CloudArmorInventory(CloudPlayer player) {
        super(player, InventoryType.ARMOR);
    }
}
