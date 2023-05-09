package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudHandInventory extends CloudInventory {

    public CloudHandInventory(CloudPlayer player) {
        super(player, InventoryType.HAND);
    }
}
