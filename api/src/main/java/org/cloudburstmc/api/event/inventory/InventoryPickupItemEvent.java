package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class InventoryPickupItemEvent extends InventoryEvent implements Cancellable {

    private final DroppedItem item;

    public InventoryPickupItemEvent(ContainerView inventory, DroppedItem item) {
        super(inventory);
        this.item = item;
    }

    public DroppedItem getItem() {
        return item;
    }
}
