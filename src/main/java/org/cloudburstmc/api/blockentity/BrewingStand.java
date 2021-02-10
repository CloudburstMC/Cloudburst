package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.BrewingInventory;

public interface BrewingStand extends BlockEntity, ContainerBlockEntity {

    @Override
    BrewingInventory getInventory();

    short getCookTime();

    void setCookTime(int cookTime);

    short getFuelAmount();

    void setFuelAmount(int fuelAmount);
}
