package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.item.ItemStack;

public interface AnvilView extends BlockContainerView {

    void setInput(ItemStack item);

    ItemStack getInput();

    void setMaterial(ItemStack item);

    ItemStack getMaterial();

    void setCost(int cost);

    int getCost();

}
