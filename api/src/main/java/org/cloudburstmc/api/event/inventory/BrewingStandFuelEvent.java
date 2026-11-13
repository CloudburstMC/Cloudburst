package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.block.BlockEvent;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Fired before a brewing stand refills its fuel. Cancelling leaves the fuel item untouched.
 */
public class BrewingStandFuelEvent extends BlockEvent implements Cancellable {

    private final ItemStack fuel;
    private int fuelPower;
    private boolean consuming = true;

    public BrewingStandFuelEvent(BrewingStand stand, ItemStack fuel, int fuelPower) {
        super(stand.getBlock());
        this.fuel = fuel;
        this.setFuelPower(fuelPower);
    }

    /**
     * @return the item providing fuel
     */
    public ItemStack getFuel() {
        return this.fuel;
    }

    /**
     * @return the number of batches this fuel will supply
     */
    public int getFuelPower() {
        return this.fuelPower;
    }

    /**
     * @param fuelPower the number of batches to supply, or zero to prevent refuelling
     */
    public void setFuelPower(int fuelPower) {
        if (fuelPower < 0) {
            throw new IllegalArgumentException("Fuel power cannot be negative");
        }
        this.fuelPower = fuelPower;
    }

    /**
     * @return whether refuelling consumes one fuel item
     */
    public boolean isConsuming() {
        return this.consuming;
    }

    /**
     * @param consuming whether to consume one fuel item on refuelling
     */
    public void setConsuming(boolean consuming) {
        this.consuming = consuming;
    }
}
