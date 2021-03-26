package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.player.Player;

public interface BeaconInventory extends Inventory {

    @Override
    Player getHolder();

}
