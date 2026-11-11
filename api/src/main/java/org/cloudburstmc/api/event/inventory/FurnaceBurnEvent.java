package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.block.BlockEvent;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Called before a furnace, blast furnace, or smoker starts burning fuel.
 */
public class FurnaceBurnEvent extends BlockEvent implements Cancellable {

    private final ItemStack fuel;
    private int burnTime;
    private boolean burning = true;
    private boolean consumeFuel = true;

    public FurnaceBurnEvent(Block block, ItemStack fuel, int burnTime) {
        super(block);
        this.fuel = Objects.requireNonNull(fuel, "fuel");
        this.burnTime = burnTime;
    }

    /**
     * @return the fuel in the fuel slot
     */
    public ItemStack getFuel() {
        return this.fuel;
    }

    /**
     * @return the burn time in ticks
     */
    public int getBurnTime() {
        return this.burnTime;
    }

    /**
     * @param burnTime the burn time in ticks. Nonpositive values prevent ignition
     */
    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    /**
     * @return whether ignition is allowed
     */
    public boolean isBurning() {
        return this.burning;
    }

    /**
     * @param burning whether to allow ignition
     */
    public void setBurning(boolean burning) {
        this.burning = burning;
    }

    /**
     * @return whether successful ignition will consume the fuel
     */
    public boolean willConsumeFuel() {
        return this.consumeFuel;
    }

    /**
     * @param consumeFuel whether successful ignition should consume the fuel
     */
    public void setConsumeFuel(boolean consumeFuel) {
        this.consumeFuel = consumeFuel;
    }
}
