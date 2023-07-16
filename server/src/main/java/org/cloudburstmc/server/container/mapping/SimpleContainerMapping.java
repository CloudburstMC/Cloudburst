package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.view.CloudArmorView;
import org.cloudburstmc.server.container.view.CloudContainerView;
import org.cloudburstmc.server.container.view.CloudInventoryView;
import org.cloudburstmc.server.container.view.CloudOffhandView;

public class SimpleContainerMapping extends ContainerMapping {

    public SimpleContainerMapping(ContainerSlotType slotType, CloudContainerView inventory) {
        super(slotType, inventory, inventory.size(), 0);
    }

    public static SimpleContainerMapping playerInventoryView(CloudInventoryView view) {
        return new SimpleContainerMapping(ContainerSlotType.INVENTORY, view);
    }

    public static SimpleContainerMapping armorView(CloudArmorView view) {
        return new SimpleContainerMapping(ContainerSlotType.ARMOR, view);
    }

    public static SimpleContainerMapping offhandView(CloudOffhandView view) {
        return new SimpleContainerMapping(ContainerSlotType.OFFHAND, view);
    }
}
