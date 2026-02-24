package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;

/**
 * Called when a player's inventory picks up an arrow from the ground.
 *
 * <p>This event operates on a {@link PlayerInventoryView} (the player's main inventory)
 * rather than an open screen, because picking up items does not require the inventory
 * screen to be open.</p>
 */
public final class InventoryPickupArrowEvent extends Event implements Cancellable {

    private final PlayerInventoryView inventory;
    private final Arrow arrow;

    public InventoryPickupArrowEvent(PlayerInventoryView inventory, Arrow arrow) {
        this.inventory = inventory;
        this.arrow = arrow;
    }

    /**
     * Returns the player inventory that will receive the arrow.
     *
     * @return the target player inventory
     */
    public PlayerInventoryView getInventory() {
        return inventory;
    }

    /**
     * Returns the arrow entity that the inventory is about to pick up.
     *
     * @return the arrow entity
     */
    public Arrow getArrow() {
        return arrow;
    }
}
