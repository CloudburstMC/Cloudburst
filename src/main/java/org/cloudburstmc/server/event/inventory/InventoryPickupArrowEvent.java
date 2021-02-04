package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.entity.impl.projectile.EntityArrow;
import org.cloudburstmc.server.inventory.Inventory;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class InventoryPickupArrowEvent extends InventoryEvent implements Cancellable {

    private final EntityArrow arrow;

    public InventoryPickupArrowEvent(Inventory inventory, EntityArrow arrow) {
        super(inventory);
        this.arrow = arrow;
    }

    public EntityArrow getArrow() {
        return arrow;
    }
}