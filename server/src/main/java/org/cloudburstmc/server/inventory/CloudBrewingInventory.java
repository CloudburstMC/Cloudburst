package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.inventory.BrewingInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;

public class CloudBrewingInventory extends CloudContainer implements BrewingInventory {

    public static final int SLOT_INGREDIENT = 0;
    public static final int SLOT_FUEL = 4;

    public CloudBrewingInventory(BrewingStand brewingStand) {
        super(brewingStand, InventoryType.BREWING_STAND);
    }

    @Override
    public BrewingStand getHolder() {
        return (BrewingStand) this.holder;
    }

    @Override
    public ItemStack getIngredient() {
        return getItem(SLOT_INGREDIENT);
    }

    @Override
    public void setIngredient(ItemStack item) {
        setItem(SLOT_INGREDIENT, item);
    }

    @Override
    public void setFuel(ItemStack fuel) {
        setItem(SLOT_FUEL, fuel);
    }

    @Override
    public ItemStack getFuel() {
        return getItem(SLOT_FUEL);
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        super.onSlotChange(index, before, send);

        this.getHolder().scheduleUpdate();
    }
}