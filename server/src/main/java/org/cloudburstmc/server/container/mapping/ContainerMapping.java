package org.cloudburstmc.server.container.mapping;

import lombok.Getter;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

public class ContainerMapping {

    @Getter
    protected final ContainerSlotType slotType;
    protected final int firstInventorySlot;
    protected final int size;
    protected final int offset;
    @Getter
    protected final SlotGroup view;

    public ContainerMapping(ContainerSlotType slotType, SlotGroup view, int size, int offset) {
        this(slotType, view, 0, size, offset);
    }

    public ContainerMapping(ContainerSlotType slotType, SlotGroup view, int firstInventorySlot, int size, int offset) {
        this.slotType = slotType;
        this.view = view;
        this.firstInventorySlot = firstInventorySlot;
        this.size = size;
        this.offset = offset;
    }

    public static ContainerMapping offhandView(OffhandView view) {
        return new ContainerMapping(ContainerSlotType.OFFHAND, view, 1, -1);
    }

    public int getInventorySlot(int screenSlot) {
        int inventorySlot = screenSlot + this.offset;
        if (inventorySlot < this.firstInventorySlot || inventorySlot >= this.firstInventorySlot + this.size) {
            throw new IllegalArgumentException("Invalid slot " + screenSlot + " for " + this.slotType);
        }

        return inventorySlot;
    }
}
