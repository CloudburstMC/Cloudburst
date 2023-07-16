package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.view.CloudContainerView;

public class LimitedContainerMapping extends ContainerMapping {

    public LimitedContainerMapping(ContainerSlotType slotType, CloudContainerView view, int size) {
        super(slotType, view, size, 0);
    }

    public LimitedContainerMapping(ContainerSlotType slotType, CloudContainerView view, int offset, int size) {
        super(slotType, view, size, offset);
    }
}
