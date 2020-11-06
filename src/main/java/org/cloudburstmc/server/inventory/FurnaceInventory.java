package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.blockentity.Furnace;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class FurnaceInventory extends ContainerInventory {

    public static final int SLOT_SMELTING = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_RESULT = 2;

    public FurnaceInventory(Furnace furnace, InventoryType inventoryType) {
        super(furnace, inventoryType);
    }

    @Override
    public Furnace getHolder() {
        return (Furnace) this.holder;
    }

    public ItemStack getResult() {
        return this.getItem(SLOT_RESULT);
    }

    public ItemStack getFuel() {
        return this.getItem(SLOT_FUEL);
    }

    public ItemStack getSmelting() {
        return this.getItem(SLOT_SMELTING);
    }

    public boolean setResult(ItemStack item) {
        return this.setItem(SLOT_RESULT, item);
    }

    public boolean setFuel(ItemStack item) {
        return this.setItem(SLOT_FUEL, item);
    }

    public boolean setSmelting(ItemStack item) {
        return this.setItem(SLOT_SMELTING, item);
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        super.onSlotChange(index, before, send);

        this.getHolder().scheduleUpdate();
    }
}
