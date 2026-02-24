package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.HotbarView;
import org.cloudburstmc.api.inventory.view.OffhandView;

/**
 * Represents the player's HUD — the always-visible hotbar and offhand slots.
 * This screen is open at all times, even when no container is open.
 */
public interface HudScreen extends InventoryScreen {

    HotbarView getHotbar();

    OffhandView getOffhand();
}
