package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * The inputs and result preview of an open smithing table.
 */
public interface SmithingView extends SlotGroup {

    /**
     * Returns the item in the template slot (the smithing template item).
     *
     * @return the template item
     */
    ItemStack getTemplate();

    /**
     * Sets the item in the template slot.
     *
     * @param item the item to place
     */
    void setTemplate(ItemStack item);

    /**
     * Returns the item to upgrade or trim.
     *
     * @return the base item
     */
    ItemStack getBase();

    /**
     * Sets the item in the base slot.
     *
     * @param item the item to place
     */
    void setBase(ItemStack item);

    /**
     * Returns the material to apply to the base item.
     *
     * @return the addition item
     */
    ItemStack getAddition();

    /**
     * Sets the item in the addition slot.
     *
     * @param item the item to place
     */
    void setAddition(ItemStack item);

    /**
     * Returns the result preview. To change it, listen for
     * {@link org.cloudburstmc.api.event.inventory.PrepareSmithingEvent}.
     *
     * @return the result item
     */
    ItemStack getResult();
}
