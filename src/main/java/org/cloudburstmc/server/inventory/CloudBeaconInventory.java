package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.BeaconInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * author: Rover656
 */
public class CloudBeaconInventory extends CloudInventory implements BeaconInventory {

    public CloudBeaconInventory(CloudPlayer player) {
        super(player, InventoryType.BEACON);
    }

    @Override
    public CloudPlayer getHolder() {
        return (CloudPlayer) super.getHolder();
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);

        //Drop item in slot
        if (this.getItem(0) != ItemStack.EMPTY) {
            this.getHolder().getLevel().dropItem(this.getHolder().getPosition(), this.getItem(0));
            this.clear(0);
        }
    }
}
