package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.PlayerInventoryView;

/**
 * Represents a full container screen that includes the player's own inventory alongside
 * the container being interacted with. All "normal" openable containers extend this interface.
 *
 * <p>The {@link #getPlayerInventory()} and {@link #getCursor()} slot groups are always present
 * in any {@code ContainerScreen}, making them accessible without a slot group lookup.</p>
 *
 * @see InventoryScreen
 * @see CursorAccess
 */
public interface ContainerScreen extends InventoryScreen, CursorAccess {

    /**
     * Returns the player's main 36-slot inventory section embedded in this container screen.
     *
     * <p><strong>Identity guarantee:</strong> this method always returns the same object as
     * {@link org.cloudburstmc.api.player.Player#getInventory()} for the player who has this
     * screen open. Code may rely on {@code screen.getPlayerInventory() == player.getInventory()}
     * being {@code true}.</p>
     *
     * @return the player's 36-slot main inventory
     */
    PlayerInventoryView getPlayerInventory();
}
