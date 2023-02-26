package org.cloudburstmc.server.inventory.view;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.inventory.CloudArmorInventory;
import org.cloudburstmc.server.inventory.CloudHandInventory;
import org.cloudburstmc.server.player.CloudPlayer;

public class SimpleInventoryView extends InventoryView {

    public SimpleInventoryView(ContainerSlotType slotType, Inventory inventory) {
        super(slotType, inventory, inventory.getSize(), 0);
    }

    public static SimpleInventoryView playerInventoryView(CloudPlayer player) {
        return new SimpleInventoryView(ContainerSlotType.INVENTORY, player.getInventory());
    }

    public static SimpleInventoryView armorView(CloudArmorInventory inventory) {
        return new SimpleInventoryView(ContainerSlotType.ARMOR, inventory);
    }

    public static SimpleInventoryView handView(CloudHandInventory inventory) {
        return new SimpleInventoryView(ContainerSlotType.OFFHAND, inventory);
    }
}
