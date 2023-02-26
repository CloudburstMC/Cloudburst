package org.cloudburstmc.server.inventory.view;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

public class InventoryView {

    protected final ContainerSlotType slotType;
    protected final int size;
    protected final int offset;
    protected final Inventory inventory;

    public InventoryView(ContainerSlotType slotType, Inventory inventory, int size, int offset) {
        this.slotType = slotType;
        this.inventory = inventory;
        this.size = size;
        this.offset = offset;
    }

    public ContainerSlotType getSlotType() {
        return slotType;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getInventorySlot(int screenSlot) {
        return screenSlot + this.offset;
    }
}
