package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents an entity's offhand slot. Any entity (including players) can have an offhand slot.
 */
public interface OffhandView extends EntitySlotGroup {

    ItemStack getOffhandItem();

    void setOffhandItem(ItemStack item);
}
