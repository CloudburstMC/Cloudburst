package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.inventory.FurnaceInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.FurnaceBlockEntity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudFurnaceInventory extends CloudContainer implements FurnaceInventory {

    public static final int SLOT_SMELTING = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_RESULT = 2;

    public CloudFurnaceInventory(Furnace furnace, InventoryType inventoryType) {
        super(furnace, inventoryType);
    }

    @Override
    public Furnace getHolder() {
        return (FurnaceBlockEntity) this.holder;
    }

    @Override
    public ItemStack getResult() {
        return this.getItem(SLOT_RESULT);
    }

    @Override
    public ItemStack getFuel() {
        return this.getItem(SLOT_FUEL);
    }

    @Override
    public ItemStack getSmelting() {
        return this.getItem(SLOT_SMELTING);
    }

    @Override
    public boolean setResult(ItemStack item) {
        return this.setItem(SLOT_RESULT, item);
    }

    @Override
    public boolean setFuel(ItemStack item) {
        return this.setItem(SLOT_FUEL, item);
    }

    @Override
    public boolean setSmelting(ItemStack item) {
        return this.setItem(SLOT_SMELTING, item);
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        super.onSlotChange(index, before, send);

        this.getHolder().scheduleUpdate();
    }
}
