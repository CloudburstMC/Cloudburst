package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * The payment slot in a beacon screen.
 */
public interface BeaconView extends SlotGroup {

    /**
     * Returns the payment item, or an empty stack.
     */
    ItemStack getPayment();

    /**
     * Sets the payment item.
     *
     * @param item the payment item, or an empty stack to clear the slot
     */
    void setPayment(ItemStack item);
}
