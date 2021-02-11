package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class CustomInventory extends CloudContainer {
    public CustomInventory(InventoryHolder holder, InventoryType type) {
        super(holder, type);
    }

    public CustomInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items) {
        super(holder, type, items);
    }

    public CustomInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items, Integer overrideSize) {
        super(holder, type, items, overrideSize);
    }

    public CustomInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> items, Integer overrideSize, String overrideTitle) {
        super(holder, type, items, overrideSize, overrideTitle);
    }
}
