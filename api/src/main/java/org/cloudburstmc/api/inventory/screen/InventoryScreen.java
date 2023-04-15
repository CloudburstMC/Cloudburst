package org.cloudburstmc.api.inventory.screen;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Collection;

public interface InventoryScreen {

    ItemStack getItem(int slot);

    void setItem(int slot, ItemStack item);

    /**
     * Retrieves the backing inventories for this screen.
     *
     * @return the backing inventories
     */
    Collection<Inventory> getInventories();
}
