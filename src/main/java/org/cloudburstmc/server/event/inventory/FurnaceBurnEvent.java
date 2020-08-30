package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.blockentity.Furnace;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.block.BlockEvent;
import org.cloudburstmc.server.item.behavior.Item;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class FurnaceBurnEvent extends BlockEvent implements Cancellable {

    private final Furnace furnace;
    private final Item fuel;
    private short burnTime;
    private boolean burning = true;

    public FurnaceBurnEvent(Furnace furnace, Item fuel, short burnTime) {
        super(furnace.getBlock());
        this.fuel = fuel;
        this.burnTime = burnTime;
        this.furnace = furnace;
    }

    public Furnace getFurnace() {
        return furnace;
    }

    public Item getFuel() {
        return fuel;
    }

    public short getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(short burnTime) {
        this.burnTime = burnTime;
    }

    public boolean isBurning() {
        return burning;
    }

    public void setBurning(boolean burning) {
        this.burning = burning;
    }
}