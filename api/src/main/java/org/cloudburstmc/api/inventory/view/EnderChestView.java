package org.cloudburstmc.api.inventory.view;

/**
 * Represents the player's ender chest storage (27 slots).
 *
 * <p>The ender chest is player-specific: each player has their own private
 * 27-slot storage accessible through any ender chest block in the world.</p>
 */
public interface EnderChestView extends PlayerSlotGroup, StorageView {
}
