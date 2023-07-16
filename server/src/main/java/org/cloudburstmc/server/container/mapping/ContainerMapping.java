package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.view.CloudContainerView;

public class ContainerMapping {

    protected final ContainerSlotType slotType;
    protected final int size;
    protected final int offset;
    protected final CloudContainerView view;

    public ContainerMapping(ContainerSlotType slotType, CloudContainerView view, int size, int offset) {
        this.slotType = slotType;
        this.view = view;
        this.size = size;
        this.offset = offset;
    }

    public ContainerSlotType getSlotType() {
        return slotType;
    }

    public CloudContainerView getView() {
        return view;
    }

    public int getInventorySlot(int screenSlot) {
        return screenSlot + this.offset;
    }
}
