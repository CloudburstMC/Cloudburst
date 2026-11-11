package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.block.BlockEvent;
import org.cloudburstmc.api.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired when a batch finishes. Cancelling leaves the bottles and ingredient unchanged.
 * The batch's fuel was already spent when it started.
 */
public class BrewEvent extends BlockEvent implements Cancellable {

    private final BrewingStand brewingStand;
    private final List<ItemStack> results;
    private final int fuelLevel;

    public BrewEvent(BrewingStand brewingStand, List<ItemStack> results, int fuelLevel) {
        super(brewingStand.getBlock());
        this.brewingStand = brewingStand;
        this.results = new ArrayList<>(results);
        this.fuelLevel = fuelLevel;
    }

    /**
     * @return the brewing stand completing the batch
     */
    public BrewingStand getBrewingStand() {
        return this.brewingStand;
    }

    /**
     * Returns the mutable results in bottle slot order. Missing entries clear the corresponding
     * slots. Entries beyond the third are ignored.
     *
     * @return the results to apply
     */
    public List<ItemStack> getResults() {
        return this.results;
    }

    /**
     * @return the number of batches remaining in the current fuel charge
     */
    public int getFuelLevel() {
        return this.fuelLevel;
    }
}
