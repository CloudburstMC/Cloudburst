package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of a smithing table container.
 * Exposes the template, base item, addition material, and result slots.
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
     * Returns the item in the base slot (the item to upgrade).
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
     * Returns the item in the addition slot (the upgrade material, e.g. netherite ingot).
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
     * Returns the item in the result slot.
     *
     * <p>This slot is computed by the client from the template, base, and addition inputs.
     * The server can read or override the result by setting this slot directly.</p>
     *
     * @return the result item
     */
    ItemStack getResult();

    /**
     * Sets the item in the result slot.
     *
     * @param item the result item to place
     */
    void setResult(ItemStack item);
}
