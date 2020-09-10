package org.cloudburstmc.server.inventory;


import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.item.ItemStack;

public class BrewingInventory extends ContainerInventory {

    public static final int SLOT_INGREDIENT = 0;
    public static final int SLOT_FUEL = 4;

    public BrewingInventory(BrewingStand brewingStand) {
        super(brewingStand, InventoryType.BREWING_STAND);
    }

    @Override
    public BrewingStand getHolder() {
        return (BrewingStand) this.holder;
    }

    public ItemStack getIngredient() {
        return getItem(SLOT_INGREDIENT);
    }

    public void setIngredient(ItemStack item) {
        setItem(SLOT_INGREDIENT, item);
    }

    public void setFuel(ItemStack fuel) {
        setItem(SLOT_FUEL, fuel);
    }

    public ItemStack getFuel() {
        return getItem(SLOT_FUEL);
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        super.onSlotChange(index, before, send);

        this.getHolder().scheduleUpdate();
    }
}