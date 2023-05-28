package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.item.ItemStack;

public interface FurnaceInventory extends Inventory {

    @Override
    Furnace getHolder();

    ItemStack getResult();

    boolean setResult(ItemStack item);

    ItemStack getFuel();

    boolean setFuel(ItemStack item);

    ItemStack getSmelting();

    boolean setSmelting(ItemStack item);
}
