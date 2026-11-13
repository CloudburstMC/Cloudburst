package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * The item, lapis, and offers of an open enchanting table.
 */
public interface EnchantingView extends SlotGroup {

    /**
     * @return the item to enchant, or an empty stack
     */
    ItemStack getInput();

    /**
     * @param item the item to enchant, or an empty stack to clear the slot
     */
    void setInput(ItemStack item);

    /**
     * @return the lapis in the table, or an empty stack
     */
    ItemStack getReagent();

    /**
     * @param item the lapis to place, or an empty stack to clear the slot
     */
    void setReagent(ItemStack item);

    /**
     * @return an immutable snapshot of the three offers in display order, with null for unavailable offers
     */
    List<@Nullable EnchantmentOffer> getOffers();

    /**
     * @return the seed used to generate the current offers
     */
    int getSeed();
}
