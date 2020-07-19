package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.player.Player;

/**
 * author: Rover656
 */
public class BeaconInventory extends FakeBlockUIComponent {

    public BeaconInventory(PlayerUIInventory playerUI, BlockState blockState) {
        super(playerUI, InventoryType.BEACON, 27, blockState);
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);

        //Drop item in slot
        this.getHolder().getLevel().dropItem(this.getHolder().getPosition(), this.getItem(0));
        this.clear(0);
    }
}
