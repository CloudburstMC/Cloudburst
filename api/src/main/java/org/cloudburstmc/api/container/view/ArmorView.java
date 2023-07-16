package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.item.ItemStack;

public interface ArmorView extends ContainerView {

    ItemStack getHelmet();

    void setHelmet(ItemStack helmet);

    ItemStack getChestplate();

    void setChestplate(ItemStack chestplate);

    ItemStack getLeggings();

    void setLeggings(ItemStack leggings);

    ItemStack getBoots();

    void setBoots(ItemStack boots);
}
