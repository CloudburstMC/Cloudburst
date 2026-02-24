package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;

public class SimpleContainerMapping extends ContainerMapping {

    public SimpleContainerMapping(ContainerSlotType slotType, SlotGroup inventory) {
        super(slotType, inventory, inventory.size(), 0);
    }

    public static SimpleContainerMapping forPlayerInventory(CloudPlayerInventory view) {
        return new SimpleContainerMapping(ContainerSlotType.INVENTORY, view);
    }

    public static SimpleContainerMapping armorView(ArmorView view) {
        return new SimpleContainerMapping(ContainerSlotType.ARMOR, view);
    }

    public static SimpleContainerMapping offhandView(OffhandView view) {
        return new SimpleContainerMapping(ContainerSlotType.OFFHAND, view);
    }
}
