package org.cloudburstmc.server.inventory.view;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

public class LimitedInventoryView extends InventoryView {
    public LimitedInventoryView(ContainerSlotType slotType, Inventory inventory, int startIndex, int size) {
        super(slotType, inventory, size, 0);
    }
}
