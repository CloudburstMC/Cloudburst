package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the player's armor equipment slots (helmet, chestplate, leggings, boots).
 */
public interface ArmorView extends EntitySlotGroup {

    ItemStack getHelmet();

    void setHelmet(ItemStack helmet);

    ItemStack getChestplate();

    void setChestplate(ItemStack chestplate);

    ItemStack getLeggings();

    void setLeggings(ItemStack leggings);

    ItemStack getBoots();

    void setBoots(ItemStack boots);
}
