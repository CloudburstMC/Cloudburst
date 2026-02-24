package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

public class ContainerMapping {

    protected final ContainerSlotType slotType;
    protected final int size;
    protected final int offset;
    protected final SlotGroup view;

    public ContainerMapping(ContainerSlotType slotType, SlotGroup view, int size, int offset) {
        this.slotType = slotType;
        this.view = view;
        this.size = size;
        this.offset = offset;
    }

    public ContainerSlotType getSlotType() {
        return slotType;
    }

    public SlotGroup getView() {
        return view;
    }

    public int getInventorySlot(int screenSlot) {
        return screenSlot + this.offset;
    }
}
