package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.InventoryScreen;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Fired after a result-slot item is calculated. Set the result to {@link ItemStack#EMPTY}
 * to prevent taking it. Preparing a result does not consume inputs.
 * In a cartography table, a custom result without a standard recipe consumes one item
 * from each occupied input slot per craft. Replacing a standard result retains its ingredient costs.
 */
public class PrepareResultEvent extends InventoryEvent {

    private ItemStack result;

    /**
     * @param screen the screen containing the result slot
     * @param result the calculated result, or an empty stack
     */
    public PrepareResultEvent(InventoryScreen screen, ItemStack result) {
        super(Objects.requireNonNull(screen, "screen"));
        this.result = Objects.requireNonNull(result, "result");
    }

    /**
     * @return the prepared result, or an empty stack
     */
    public ItemStack getResult() {
        return this.result;
    }

    /**
     * Changes the prepared item without consuming inputs.
     *
     * @param result the replacement result, or an empty stack to disable taking it
     */
    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }
}
