package org.cloudburstmc.api.inventory.screen;

import org.cloudburstmc.api.inventory.PlayerInventory;
import org.cloudburstmc.api.inventory.WorkbenchInventory;

public interface PlayerInventoryScreen {

    PlayerInventory getPlayerInventory();

    WorkbenchInventory getWorkbenchInventory();


}
