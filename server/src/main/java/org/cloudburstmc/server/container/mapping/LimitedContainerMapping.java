package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

public class LimitedContainerMapping extends ContainerMapping {

    public LimitedContainerMapping(ContainerSlotType slotType, SlotGroup view, int size) {
        super(slotType, view, size, 0);
    }
}
