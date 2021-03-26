package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.item.ItemStack;

public interface BrewingInventory extends ContainerInventory {

    ItemStack getIngredient();

    void setIngredient(ItemStack item);

    ItemStack getFuel();

    void setFuel(ItemStack fuel);

    @Override
    BrewingStand getHolder();

    @Override
    default InventoryType getType() {
        return InventoryType.BREWING_STAND;
    }
}
