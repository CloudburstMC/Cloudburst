package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.Inventory;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class InventoryPickupArrowEvent extends InventoryEvent implements Cancellable {

    private final Arrow arrow;

    public InventoryPickupArrowEvent(Inventory inventory, Arrow arrow) {
        super(inventory);
        this.arrow = arrow;
    }

    public Arrow getArrow() {
        return arrow;
    }
}