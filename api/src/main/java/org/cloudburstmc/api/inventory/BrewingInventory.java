package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;

public interface BrewingInventory extends Inventory {

    ItemStack getIngredient();

    ItemStack getFuel();

    @Override
    default InventoryType getType() {
        return InventoryType.BREWING_STAND;
    }
}
