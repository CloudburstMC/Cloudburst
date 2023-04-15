package org.cloudburstmc.api.inventory.screen;

import org.cloudburstmc.api.inventory.PlayerInventory;

public interface HudInventoryScreen extends InventoryScreen {

    PlayerInventory getPlayerInventory();
}
