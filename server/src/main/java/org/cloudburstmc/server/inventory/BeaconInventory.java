package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.player.Player;

/**
 * author: Rover656
 */
public class BeaconInventory extends FakeBlockUIComponent {

    public BeaconInventory(PlayerUIInventory playerUI, Block block) {
        super(playerUI, InventoryType.BEACON, 27, block);
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);

        //Drop item in slot
        if (!this.getItem(0).isNull()) {
            this.getHolder().getLevel().dropItem(this.getHolder().getPosition(), this.getItem(0));
            this.clear(0);
        }
    }
}
