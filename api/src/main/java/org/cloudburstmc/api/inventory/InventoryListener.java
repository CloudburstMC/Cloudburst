package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;

public interface InventoryListener {

    void onInventoryAdded(Inventory inventory);

    void onInventoryRemoved(Inventory inventory);

    void onInventorySlotChange(Inventory inventory, int slot, ItemStack itemStack);

    void onInventoryContentsChange(Inventory inventory);

    void onInventoryDataChange(Inventory inventory, int property, int value);
}
