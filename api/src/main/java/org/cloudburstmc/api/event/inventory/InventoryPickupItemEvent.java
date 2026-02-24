package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;

/**
 * Called when a player's inventory picks up a dropped item from the ground.
 *
 * <p>This event operates on a {@link PlayerInventoryView} (the player's main inventory)
 * rather than an open screen, because picking up items does not require the inventory
 * screen to be open.</p>
 */
public final class InventoryPickupItemEvent extends Event implements Cancellable {

    private final PlayerInventoryView inventory;
    private final DroppedItem item;

    public InventoryPickupItemEvent(PlayerInventoryView inventory, DroppedItem item) {
        this.inventory = inventory;
        this.item = item;
    }

    /**
     * Returns the player inventory that will receive the dropped item.
     *
     * @return the target player inventory
     */
    public PlayerInventoryView getInventory() {
        return inventory;
    }

    /**
     * Returns the dropped item entity that the inventory is about to pick up.
     *
     * @return the dropped item entity
     */
    public DroppedItem getItem() {
        return item;
    }
}
